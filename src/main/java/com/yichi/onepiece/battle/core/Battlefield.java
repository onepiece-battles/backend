package com.yichi.onepiece.battle.core;

import com.yichi.onepiece.battle.model.BattleProcessDetailEntity;
import com.yichi.onepiece.battle.model.BattleProcessDetailEntityBuilder;
import com.yichi.onepiece.battle.model.BattleStatusDetailEntity;
import com.yichi.onepiece.battle.model.BattleStatusDetailEntityBuilder;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.utils.RandomGenerator;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Battlefield {
  private static final Logger logger = LoggerFactory.getLogger(Battlefield.class);
  private static final int MAXIMUM_INTELLIGENCE = 260;
  private static final int MINIMUM_INTELLIGENCE = 70;
  private static final int MAXIMUM_SPEED = 200;
  private static final int MINIMUM_SPEED = 10;

  // hide index is 100 based.
  private static final int BASE_HIDE_INDEX = 30;
  private static final int READ_HIDE_INDEX_WEIGHT = 70;

  // hide index is speed and intelligence related and 100 based.
  private int MAX_HIDE_INDEX_DIFF;
  private Map<UUID, Integer> hideIndexMap;
  private Map<UUID, CharacterInBattle> idToCharacterMap;
  private Map<UUID, Boolean> characterReverseMap;
  private final UUID battleId;
  private final CharacterInBattle characterInit;
  private final CharacterInBattle characterReceive;
  private final List<UUID> characterIds;
  private List<BattleStatusDetailEntity> battleStatusDetailEntities;
  private List<BattleProcessDetailEntity> battleProcessDetailEntities;

  private UUID currentAttackerId;
  private UUID currentDefenderId;

  private Map<UUID, Integer> attackLikelihoodMap;

  private int battleStatusCount;
  private int battleProcessCount;

  public Battlefield(
      CharacterInBattle characterInit, CharacterInBattle characterReceive, UUID battleId) {
    this.characterInit = characterInit;
    this.characterReceive = characterReceive;
    this.battleId = battleId;
    this.currentAttackerId = characterInit.getCharacterId();
    this.currentDefenderId = characterReceive.getCharacterId();
    characterIds = List.of(characterInit.getCharacterId(), characterReceive.getCharacterId());
    initBattle(characterInit, characterReceive);
  }

  private void initBattle(CharacterInBattle characterInit, CharacterInBattle characterReceive) {
    characterReverseMap = new HashMap<>();
    idToCharacterMap = new HashMap<>();
    logger.info("create attack member list");
    battleStatusDetailEntities = new ArrayList<>();
    battleProcessDetailEntities = new ArrayList<>();
    battleStatusCount = 0;
    battleProcessCount = 0;
    logger.info("create character reverse map");
    characterReverseMap.put(characterInit.getCharacterId(), Boolean.FALSE);
    characterReverseMap.put(characterReceive.getCharacterId(), Boolean.FALSE);
    idToCharacterMap.put(characterInit.getCharacterId(), characterInit);
    idToCharacterMap.put(characterReceive.getCharacterId(), characterReceive);
    logger.info("create hide index map");
    this.MAX_HIDE_INDEX_DIFF = MAXIMUM_SPEED - MINIMUM_SPEED;
    calculateHideIndex();
    calculateAttackLikelihood();
  }

  private void calculateHideIndex() {
    hideIndexMap = new HashMap<>();
    UUID initId = characterInit.getCharacterId();
    int initSpeed = characterInit.getSpeed();
    int initIntelligence = characterInit.getIntelligence();
    UUID receiveId = characterReceive.getCharacterId();
    int receiveSpeed = characterReceive.getSpeed();
    int receiveIntelligence = characterReceive.getIntelligence();
    int REAL_HIDE_INDEX_DIFF =
        Math.abs(initSpeed + initIntelligence - receiveSpeed - receiveIntelligence);
    int initHideIndex = BASE_HIDE_INDEX;
    int receiveHideIndex = BASE_HIDE_INDEX;
    if ((initSpeed + initIntelligence) > (receiveSpeed - receiveIntelligence)) {
      initHideIndex =
          (BASE_HIDE_INDEX + REAL_HIDE_INDEX_DIFF / MAX_HIDE_INDEX_DIFF * READ_HIDE_INDEX_WEIGHT);
      receiveHideIndex =
          (BASE_HIDE_INDEX - REAL_HIDE_INDEX_DIFF / MAX_HIDE_INDEX_DIFF * READ_HIDE_INDEX_WEIGHT);
      receiveHideIndex = Math.max(receiveHideIndex, 0);
    }
    if ((initSpeed + initIntelligence) < (receiveSpeed - receiveIntelligence)) {
      receiveHideIndex =
          (BASE_HIDE_INDEX + REAL_HIDE_INDEX_DIFF / MAX_HIDE_INDEX_DIFF * READ_HIDE_INDEX_WEIGHT);
      initHideIndex =
          (BASE_HIDE_INDEX - REAL_HIDE_INDEX_DIFF / MAX_HIDE_INDEX_DIFF * READ_HIDE_INDEX_WEIGHT);
      initHideIndex = Math.max(initHideIndex, 0);
    }
    hideIndexMap.put(initId, initHideIndex);
    hideIndexMap.put(receiveId, receiveHideIndex);
  }

  private void calculateAttackLikelihood() {
    attackLikelihoodMap = new HashMap<>();
    int attackEnhance =
            (int)
                    (200
                            * ((double)
                            Math.abs(
                                    characterInit.getIntelligence() - characterReceive.getIntelligence())
                            / MAXIMUM_INTELLIGENCE));
    if (characterInit.getIntelligence() > characterReceive.getIntelligence()) {
      attackLikelihoodMap.put(characterInit.getCharacterId(), 100 + attackEnhance);
      attackLikelihoodMap.put(characterReceive.getCharacterId(), 100);
    } else {
      attackLikelihoodMap.put(characterInit.getCharacterId(), 100);
      attackLikelihoodMap.put(characterReceive.getCharacterId(), 100 + attackEnhance);
    }
  }

  public boolean allAlive() {
    return characterInit.isAlive() && characterReceive.isAlive();
  }

  public CharacterInBattle getWinner() {
    if (characterInit.isAlive()) {
      return characterInit;
    } else {
      return characterReceive;
    }
  }

  public CharacterInBattle getLoser() {
    if (characterInit.isAlive()) {
      return characterReceive;
    } else {
      return characterInit;
    }
  }

  public void completeFinalStatus() {
    battleStatusCount++;
    BattleStatusDetailEntity battleStatusDetailEntity =
        BattleStatusDetailEntityBuilder.newBuilder()
            .setBattleId(battleId)
            .setBattleStatusRoundNumber(battleStatusCount)
            .setInitSide(characterInit.getCurrentCharacterEntity())
            .setReceiveSide(characterReceive.getCurrentCharacterEntity())
            .build();
    battleStatusDetailEntities.add(battleStatusDetailEntity);
  }

  public List<BattleProcessDetailEntity> getBattleProcessDetailEntities() {
    return this.battleProcessDetailEntities;
  }

  public List<BattleStatusDetailEntity> getBattleStatusDetailEntities() {
    return battleStatusDetailEntities;
  }

  public void processBattle() {
    battleStatusCount++;
    battleProcessCount++;
    var attackSide = getAttackSide();
    var defenseSide = getDefenseSide();
    BattleStatusDetailEntity battleStatusDetailEntity =
        BattleStatusDetailEntityBuilder.newBuilder()
            .setBattleId(battleId)
            .setBattleStatusRoundNumber(battleStatusCount)
            .setInitSide(characterInit.getCurrentCharacterEntity())
            .setReceiveSide(characterReceive.getCurrentCharacterEntity())
            .build();
    battleStatusDetailEntities.add(battleStatusDetailEntity);
    BattleProcessDetailEntityBuilder battleProcessDetailEntityBuilder =
        BattleProcessDetailEntityBuilder.newBuilder();
    battleProcessDetailEntityBuilder
        .setBattleId(battleId)
        .setAttackerId(currentAttackerId)
        .setBattleProcessRoundNumber(battleProcessCount)
        .setAttackerEnergyLoss(attackSide.getEnergyConsume())
        .setAttackSkill("");
    var attackEntity = attackSide.attack();
    if (attackEntity.getAttack()==-1) {
      return;
    }
    // update attacker information
    if (!hideSuccess()) {
      battleProcessDetailEntityBuilder.setAttackMissed(Boolean.FALSE);
      // the order of return list is: blood loss, magic loss and energy loss.
      var defenseResult = defenseSide.defend(attackEntity);
      battleProcessDetailEntityBuilder
          .setDefenderBloodLoss(defenseResult.get(0))
          .setDefenderEnergyLoss(defenseResult.get(1));
    } else {
      battleProcessDetailEntityBuilder.setAttackMissed(Boolean.TRUE).setDefenderBloodLoss(0).setDefenderEnergyLoss(0);
    }
    battleProcessDetailEntities.add(battleProcessDetailEntityBuilder.build());
  }

  public void checkReverse() {
    if (characterInit.getReversed() && !characterReverseMap.get(characterInit.getCharacterId())) {
      updateBattleIndex();
      characterReverseMap.put(characterInit.getCharacterId(), Boolean.TRUE);
    }
    if (characterReceive.getReversed()
        && !characterReverseMap.get(characterReceive.getCharacterId())) {
      updateBattleIndex();
      characterReverseMap.put(characterReceive.getCharacterId(), Boolean.TRUE);
    }
  }

  // this function is used to determine which character got the chance to attack.
  // |------------attack---------|----defend----| which zone the number locate in determines which side got the chance
  // to attack.
  public void refreshAttackSide() {
    int value = RandomGenerator.generateInteger(
            1,
            attackLikelihoodMap.get(characterInit.getCharacterId())+ attackLikelihoodMap.get(characterReceive.getCharacterId()));
    if (value<=attackLikelihoodMap.get(characterInit.getCharacterId())) {
      logger.info("init");
      currentAttackerId = characterInit.getCharacterId();
      currentDefenderId = characterReceive.getCharacterId();
    } else {
      logger.info("receive");
      currentDefenderId = characterInit.getCharacterId();
      currentAttackerId = characterReceive.getCharacterId();
    }
  }

  private boolean hideSuccess() {
    int hideIndex = hideIndexMap.get(characterIds.get(1));
    return RandomGenerator.generateInteger(1, 100) < hideIndex;
  }

  private void updateBattleIndex() {
    calculateHideIndex();
    calculateAttackLikelihood();
  }

  public CharacterInBattle getAttackSide() {
    return idToCharacterMap.get(currentAttackerId);
  }

  public CharacterInBattle getDefenseSide() {
    return idToCharacterMap.get(currentDefenderId);
  }
}
