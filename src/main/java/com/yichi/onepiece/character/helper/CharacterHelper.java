package com.yichi.onepiece.character.helper;

import com.yichi.onepiece.character.model.dao.CharacterDao;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import java.util.List;
import java.util.UUID;

import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.model.FruitEntity;
import com.yichi.onepiece.fruit.model.FruitListView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CharacterHelper {
  Logger logger = LoggerFactory.getLogger(CharacterHelper.class);
  public static final Integer ATTACK_MIN = 10;
  public static final Integer ATTACK_MAX = 1500;
  public static final Integer DEFENSE_MIN = 10;
  public static final Integer DEFENSE_MAX = 1400;
  public static final Integer BLOOD_MIN = 100;
  public static final Integer BLOOD_MAX = 18000;
  public static final Integer SPEED_MIN = 10;
  public static final Integer SPEED_MAX = 300;
  public static final Integer GROUP_GAIN_MIN = 80;
  public static final Integer GROUP_GAIN_MAX = 300;
  public static final Integer INTELLIGENCE_MIN = 50;
  public static final Integer INTELLIGENCE_MAX = 300;
  private final CharacterDao characterDao;

  public CharacterHelper(CharacterDao characterDao) {
    this.characterDao = characterDao;
  }

  // list all characters that eat this fruit.
  public List<CharacterEntity> listByFruitId(UUID fruitId) {
    return characterDao.listByFruitId(fruitId);
  }



  public void calculateCharacterBattleIndex(CharacterEntity character, List<FruitEntity> fruits) {
    boolean firstFruit = true;
    for(var fruit: fruits) {
      if (firstFruit) {
        character =
                character.toBuilder()
                        .setAttack(
                                normalizeValue(ATTACK_MIN, ATTACK_MAX,
                                        (int) (character.getBaseAttack() * fruit.getAttackSlope() + fruit.getAttackIntercept()))
                        )
                        .setBlood(
                                normalizeValue(BLOOD_MIN, BLOOD_MAX,
                                        (int) (character.getBaseBlood() * fruit.getBloodSlope() + fruit.getBloodIntercept()))
                        )
                        .setSpeed(
                                normalizeValue(SPEED_MIN, SPEED_MAX,
                                        (int) (character.getBaseSpeed() * fruit.getSpeedSlope() + fruit.getSpeedIntercept()))
                        )
                        .setGroupGain(
                                normalizeValue(GROUP_GAIN_MIN, GROUP_GAIN_MAX,
                                        (int) (character.getBaseGroupGain() * fruit.getGroupGainSlope() + fruit.getGroupGainIntercept())))
                        .setDefense(
                                normalizeValue(DEFENSE_MIN, DEFENSE_MAX,
                                        (int) (character.getBaseDefense() * fruit.getDefenseSlope() + fruit.getDefenseIntercept())))
                        .setIntelligence(
                                normalizeValue(INTELLIGENCE_MIN, INTELLIGENCE_MAX,
                                        (int) (character.getBaseIntelligence() * fruit.getIntelligenceSlope() + fruit.getIntelligenceIntercept())))
                        .build();
        firstFruit = false;
      } else {
        character =
                character.toBuilder()
                        .setAttack(
                                normalizeValue(ATTACK_MIN, ATTACK_MAX,
                                        (int) (character.getAttack() * fruit.getAttackSlope() + fruit.getAttackIntercept()))
                        )
                        .setBlood(
                                normalizeValue(BLOOD_MIN, BLOOD_MAX,
                                        (int) (character.getBlood() * fruit.getBloodSlope() + fruit.getBloodIntercept()))
                        )
                        .setSpeed(
                                normalizeValue(SPEED_MIN, SPEED_MAX,
                                        (int) (character.getSpeed() * fruit.getSpeedSlope() + fruit.getSpeedIntercept()))
                        )
                        .setGroupGain(
                                normalizeValue(GROUP_GAIN_MIN, GROUP_GAIN_MAX,
                                        (int) (character.getGroupGain() * fruit.getGroupGainSlope() + fruit.getGroupGainIntercept())))
                        .setDefense(
                                normalizeValue(DEFENSE_MIN, DEFENSE_MAX,
                                        (int) (character.getDefense() * fruit.getDefenseSlope() + fruit.getDefenseIntercept())))
                        .setIntelligence(
                                normalizeValue(INTELLIGENCE_MIN, INTELLIGENCE_MAX,
                                        (int) (character.getIntelligence() * fruit.getIntelligenceSlope() + fruit.getIntelligenceIntercept())))
                        .build();
      }
    }
    logger.info("character update {}", character);
    characterDao.update(character);
  }

  private int normalizeValue(int low, int high, int value) {
    if (value < low) {
      return low;
    } else return Math.min(value, high);
  }
}
