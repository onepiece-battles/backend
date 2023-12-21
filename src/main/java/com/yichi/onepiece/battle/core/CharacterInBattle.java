package com.yichi.onepiece.battle.core;

import com.yichi.onepiece.battle.model.CurrentCharacterEntityBuilder;
import com.yichi.onepiece.battle.model.CurrentCharacterEntityImpl;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.fruit.model.FruitEntity;
import com.yichi.onepiece.fruit.model.FruitListView;
import com.yichi.onepiece.model.I18nText;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharacterInBattle {
  Logger logger = LoggerFactory.getLogger(CharacterInBattle.class);
  private static final int INIT_ENERGY_PERCENTAGE = 100;
  private static final int MINIMUM_FRUIT_ENERGY = 15;
  private final UUID characterId;
  private final I18nText name;
  private int intelligence;
  private int magicConsume;
  private boolean defeated;
  private boolean smarter;
  private boolean reversed;
  private AttackEntity attackEntity;
  private final CharacterEntity baseEntity;
  private CurrentCharacterEntityImpl currentCharacterEntity;

  List<FruitListView> fruitEntities;

  public CharacterInBattle(CharacterEntity baseEntity, List<FruitListView> fruitEntities) {
    this.baseEntity = baseEntity;
    this.fruitEntities = fruitEntities;
    this.name = baseEntity.getName();
    this.defeated = false;
    this.reversed = false;
    this.smarter = false;
    this.characterId = baseEntity.getId();
    initWithFruit(baseEntity);
    initAttackEntity();
  }

  private void initWithFruit(CharacterEntity baseEntity) {
    int blood = baseEntity.getBlood();
    int attack = baseEntity.getAttack();
    int defense = baseEntity.getDefense();
    int intelligence = baseEntity.getIntelligence();
    int speed = baseEntity.getSpeed();
    int energyConsume = 0;
    // add fruit strengthen if this character eats fruits.
    if (!fruitEntities.isEmpty()) {
      energyConsume = fruitEntities.stream().mapToInt(FruitEntity::getEnergyConsume).sum();
      for (FruitEntity fruit : fruitEntities) {
        blood = (int) (blood * fruit.getBloodStrengthen());
        attack = (int) (attack * fruit.getAttackStrengthen());
        intelligence = (int) (intelligence * fruit.getIntelligenceStrengthen());
        defense = (int) (defense * fruit.getDefenseStrengthen());
        speed = (int) (speed * fruit.getSpeedStrengthen());
      }
    }
    this.intelligence = intelligence;
    this.magicConsume = energyConsume;
    this.currentCharacterEntity =
        (CurrentCharacterEntityImpl)
            CurrentCharacterEntityBuilder.newBuilder()
                .setAttack(attack)
                .setBlood(blood)
                .setDefense(defense)
                .setEnergyPercentage(INIT_ENERGY_PERCENTAGE)
                .setSpeed(speed)
                .build();
  }

  private void initAttackEntity() {
    this.attackEntity =
        AttackEntityBuilder.newBuilder().setAttack(currentCharacterEntity.getAttack()).build();
  }

  public boolean getReversed() {
    return reversed;
  }

  public boolean isAlive() {
    return !defeated;
  }

  public int getBlood() {
    return currentCharacterEntity.getBlood();
  }

  public int getSpeed() {
    return currentCharacterEntity.getSpeed();
  }

  public I18nText getName() {
    return name;
  }

  public boolean getSmarter() {
    return smarter;
  }

  public int getMagicConsume() {
    return magicConsume;
  }

  public void setSmarter(boolean smarter) {
    this.smarter = smarter;
  }

  public UUID getCharacterId() {
    return characterId;
  }

  public int getIntelligence() {
    return intelligence;
  }

  public AttackEntity attack() {
    if (currentCharacterEntity.getEnergyPercentage() < MINIMUM_FRUIT_ENERGY && !reversed) {
      reverseCharacter();
      reversed = true;
      logger.info("run out of magic, reverse {}", name);
    }
    logger.info("{} attack with power {}", name, currentCharacterEntity.getEnergyPercentage());
    return attackEntity;
  }

  // the order of return list is: blood loss, magic loss and energy loss.
  public List<Integer> defense(AttackEntity attackEntity) {
    if (!reversed) {
      reverseCharacter();
      reversed = true;
      logger.info("run out of magic, reverse {}", name);
    }
    int blood = currentCharacterEntity.getBlood();
    if (blood <= 0) {
      this.defeated = true;
      return List.of(0, 0, 0);
    }
    int bloodLoss = Math.max(attackEntity.getAttack() - currentCharacterEntity.getDefense(), 0);
    currentCharacterEntity.setBlood(blood - bloodLoss);
    logger.info("{} defense with bloodLoss {}", name, bloodLoss);
    return List.of(bloodLoss, 0, 0);
  }

  public CurrentCharacterEntityImpl getCurrentCharacterEntity() {
    return currentCharacterEntity;
  }

  private void reverseCharacter() {
    // blood strengthen can not be reversed.
    logger.info("{} is reversed, attack, defense, intelligence, speed are reset", name);
    currentCharacterEntity.setAttack(baseEntity.getAttack());
    currentCharacterEntity.setDefense(baseEntity.getDefense());
    currentCharacterEntity.setSpeed(baseEntity.getSpeed());
    this.intelligence = baseEntity.getIntelligence();
    // refresh attackEntity
    attackEntity = attackEntity.toBuilder().setAttack(currentCharacterEntity.getAttack()).build();
  }
}
