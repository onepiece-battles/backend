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
  // attack index is intelligence related and 100 based.
  private int attackRepeatIndex;
  private List<UUID> battleParticipantsIds;
  private Map<UUID, CharacterInBattle> idToCharacterMap;
  private Map<UUID, Boolean> characterReverseMap;
  private final UUID battleId;
  private final CharacterInBattle characterInit;
  private final CharacterInBattle characterReceive;
  private List<BattleStatusDetailEntity> battleStatusDetailEntities;
  private List<BattleProcessDetailEntity> battleProcessDetailEntities;

  private int battleStatusCount;
  private int battleProcessCount;

  public Battlefield(
      CharacterInBattle characterInit, CharacterInBattle characterReceive, UUID battleId) {
    this.characterInit = characterInit;
    this.characterReceive = characterReceive;
    this.battleId = battleId;
    initBattle();
  }

  private void initBattle() {
    logger.info("add id to character map");
    idToCharacterMap = new HashMap<>();
    idToCharacterMap.put(characterInit.getCharacterId(), characterInit);
    idToCharacterMap.put(characterReceive.getCharacterId(), characterReceive);
    logger.info("create attack member list");
    battleStatusDetailEntities = new ArrayList<>();
    battleProcessDetailEntities = new ArrayList<>();
    battleStatusCount = 0;
    battleProcessCount = 0;
    battleParticipantsIds = new ArrayList<>(2);
    battleParticipantsIds.addAll(
        List.of(characterInit.getCharacterId(), characterReceive.getCharacterId()));
    logger.info("create character reverse map");
    characterReverseMap = new HashMap<>();
    characterReverseMap.put(characterInit.getCharacterId(), Boolean.FALSE);
    characterReverseMap.put(characterReceive.getCharacterId(), Boolean.FALSE);
    logger.info("create hide index map");
    this.MAX_HIDE_INDEX_DIFF =
        MAXIMUM_INTELLIGENCE + MAXIMUM_SPEED - MINIMUM_SPEED - MINIMUM_INTELLIGENCE;
    calculateHideIndex();
    logger.info("calculate attack repeat index");
    calculateAttackRepeatIndex();
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

  private void calculateAttackRepeatIndex() {
    if (characterInit.getIntelligence() > characterReceive.getIntelligence()) {
      characterInit.setSmarter(true);
      characterReceive.setSmarter(false);
    }
    if (characterInit.getIntelligence() < characterReceive.getIntelligence()) {
      characterInit.setSmarter(false);
      characterReceive.setSmarter(true);
    }
    // intelligence advantage expands the opportunity to attack.
    if (characterInit.getSmarter() || characterReceive.getSmarter()) {
      this.attackRepeatIndex =
          (int)
              (100
                  * ((double)
                          Math.abs(
                              characterInit.getIntelligence() - characterReceive.getIntelligence())
                      / MAXIMUM_INTELLIGENCE));
      logger.info("attack repeat index is set to {}", this.attackRepeatIndex);
    } else {
      this.attackRepeatIndex = 0;
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
            .setAttackSide(new I18nText())
            .setAttacker(characterInit.getCurrentCharacterEntity())
            .setDefender(characterReceive.getCurrentCharacterEntity())
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
    // TODO: 2023/8/25 check energy condition before next battle
    logger.info("--------------------new battle process start----------------------------");
    battleStatusCount++;
    battleProcessCount++;
    var attackSide = getAttackSide();
    var defense = getDefenseSide();
    BattleStatusDetailEntity battleStatusDetailEntity =
        BattleStatusDetailEntityBuilder.newBuilder()
            .setBattleId(battleId)
            .setBattleStatusRoundNumber(battleStatusCount)
            .setAttackSide(attackSide.getName())
            .setAttacker(attackSide.getCurrentCharacterEntity())
            .setDefender(defense.getCurrentCharacterEntity())
            .build();
    battleStatusDetailEntities.add(battleStatusDetailEntity);
    logger.info("attack side: {}, defense side: {}", attackSide.getName(), defense.getName());
    BattleProcessDetailEntityBuilder battleProcessDetailEntityBuilder =
        BattleProcessDetailEntityBuilder.newBuilder();
    battleProcessDetailEntityBuilder
        .setBattleId(battleId)
        .setAttackerEnergyLoss(1)
        .setBattleProcessRoundNumber(battleProcessCount)
        .setAttackerEnergyLoss(attackSide.getMagicConsume())
        .setAttackSkill("");

    var attackEntity = attackSide.attack();
    if (!hideSuccess()) {
      battleProcessDetailEntityBuilder.setAttackMissed(Boolean.FALSE);
      // the order of return list is: blood loss, magic loss and energy loss.
      var defenseResult = defense.defense(attackEntity);
      battleProcessDetailEntityBuilder
          .setDefenderBloodLoss(defenseResult.get(0))
          .setDefenderEnergyLoss(defenseResult.get(2));
    } else {
      battleProcessDetailEntityBuilder.setAttackMissed(Boolean.TRUE);
      logger.info("{} hide SUCCESS, not damage is casted", defense.getName());
    }
    battleProcessDetailEntities.add(battleProcessDetailEntityBuilder.build());
    logger.info("---------------------new battle process end-----------------------------");
  }

  public void checkReverse() {
    if (characterInit.getReversed() && !characterReverseMap.get(characterInit.getCharacterId())) {
      reverseBattlefield(characterInit.getCharacterId());
      characterReverseMap.put(characterInit.getCharacterId(), Boolean.TRUE);
    }
    if (characterReceive.getReversed()
        && !characterReverseMap.get(characterReceive.getCharacterId())) {
      reverseBattlefield(characterReceive.getCharacterId());
      characterReverseMap.put(characterReceive.getCharacterId(), Boolean.TRUE);
    }
  }

  public void attackReset() {
    // if the attack side get the chance to attack again:
    if (idToCharacterMap.get(battleParticipantsIds.get(0)).getSmarter()) {
      if (RandomGenerator.generateInteger(1, 100) <= attackRepeatIndex) {
        logger.info("attack/defense continue...");
        return;
      }
    }
    // otherwise swap the attack and defense
    var temp = battleParticipantsIds.get(0);
    battleParticipantsIds.set(0, battleParticipantsIds.get(1));
    battleParticipantsIds.set(1, temp);
    logger.info("attack/defense reverse...");
  }

  private boolean hideSuccess() {
    int hideIndex = hideIndexMap.get(battleParticipantsIds.get(1));
    return RandomGenerator.generateInteger(1, 100) < hideIndex;
  }

  private void reverseBattlefield(UUID characterId) {
    String name = idToCharacterMap.get(characterId).getName().toString();
    logger.info("{} is reversed, reverse battle field", name);
    logger.info("recalculate hide index...");
    calculateHideIndex();
    logger.info("hide index has been reset to be {}", hideIndexMap.get(characterId));
    logger.info("recalculate attack repeat index...");
    calculateAttackRepeatIndex();
    logger.info("attack repeat index has been reset to be {}", attackRepeatIndex);
  }

  public CharacterInBattle getAttackSide() {
    return idToCharacterMap.get(battleParticipantsIds.get(0));
  }

  public CharacterInBattle getDefenseSide() {
    return idToCharacterMap.get(battleParticipantsIds.get(1));
  }
}
