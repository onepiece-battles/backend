package com.yichi.onepiece.battle.skill;

import static com.yichi.onepiece.battle.skill.CombatModels.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.battle.core.Battlefield;
import com.yichi.onepiece.battle.core.CharacterInBattle;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class CombatSession {
  private final CombatInputs inputs;
  private final com.yichi.onepiece.battle.model.BattleProcessDetailDao processDao;
  private final com.yichi.onepiece.battle.model.BattleStatusDetailDao statusDao;
  private final BattleTactics tactics;
  private final CombatRecordDao records;
  private final Clock clock;
  private final ObjectMapper json = new ObjectMapper();

  public CombatSession(
      CombatInputs inputs,
      BattleTactics tactics,
      CombatRecordDao records,
      Clock clock,
      com.yichi.onepiece.battle.model.BattleProcessDetailDao processDao,
      com.yichi.onepiece.battle.model.BattleStatusDetailDao statusDao) {
    this.inputs = inputs;
    this.tactics = tactics;
    this.records = records;
    this.clock = clock;
    this.processDao = processDao;
    this.statusDao = statusDao;
  }

  public Battlefield open(UUID battleId, UUID firstId, UUID secondId, String locale) {
    var input = inputs.freeze(firstId, secondId);
    var first = input.first();
    var second = input.second();
    var firstFruits = input.firstFruits();
    var secondFruits = input.secondFruits();
    var firstHaki = input.firstHakis();
    var secondHaki = input.secondHakis();
    var frozen = input.profiles();
    var attributes = new LinkedHashMap<UUID, Map<String, Integer>>();
    for (var actor : List.of(first, second))
      attributes.put(
          actor.getId(),
          Map.of(
              "attack",
              actor.getAttack(),
              "defense",
              actor.getDefense(),
              "blood",
              actor.getBlood(),
              "energy",
              actor.getEnergy(),
              "speed",
              actor.getSpeed(),
              "intelligence",
              actor.getIntelligence()));
    var strategy = tactics.plan(frozen, attributes);
    long seed = new java.security.SecureRandom().nextLong();
    var snapshot =
        json.createObjectNode()
            .put("engineVersion", ENGINE_VERSION)
            .put("seed", Long.toString(seed))
            .put("locale", locale);
    snapshot.set("starter", json.valueToTree(first));
    snapshot.set("receiver", json.valueToTree(second));
    snapshot.set("starterFruits", json.valueToTree(firstFruits));
    snapshot.set("receiverFruits", json.valueToTree(secondFruits));
    snapshot.set("starterHakis", json.valueToTree(firstHaki));
    snapshot.set("receiverHakis", json.valueToTree(secondHaki));
    snapshot.set("profiles", json.valueToTree(frozen));
    snapshot.set("tactics", json.valueToTree(strategy));
    records.insert(battleId, ENGINE_VERSION, seed, snapshot.toString(), clock.millis());
    return new Battlefield(
        new CharacterInBattle(first, firstFruits, frozen.get(first.getId()), firstHaki),
        new CharacterInBattle(second, secondFruits, frozen.get(second.getId()), secondHaki),
        battleId,
        seed,
        locale,
        strategy);
  }

  @org.springframework.transaction.annotation.Transactional
  public void complete(UUID id, Battlefield battlefield) {
    complete(id, battlefield, () -> {});
  }

  @org.springframework.transaction.annotation.Transactional
  public void complete(UUID id, Battlefield battlefield, Runnable saveResult) {
    try {
      inputs.lockParticipants(
          battlefield.getAttackSide().getCharacterId(),
          battlefield.getDefenseSide().getCharacterId());
      statusDao.batchInsert(battlefield.getBattleStatusDetailEntities());
      processDao.batchInsert(battlefield.getBattleProcessDetailEntities());
      if (records.complete(id, json.writeValueAsString(battlefield.events())) != 1)
        throw new IllegalStateException("Battle events already finalized or snapshot missing");
      saveResult.run();
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Cannot save battle events", e);
    }
  }

  public Map<String, JsonNode> read(UUID id) {
    try {
      return Map.of(
          "snapshot",
          json.readTree(records.snapshot(id).orElse("null")),
          "events",
          json.readTree(records.events(id).orElse("[]")));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Invalid combat record", e);
    }
  }
}
