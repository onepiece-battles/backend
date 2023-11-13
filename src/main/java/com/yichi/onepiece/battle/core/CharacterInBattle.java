package com.yichi.onepiece.battle.core;

import com.yichi.onepiece.battle.model.CurrentCharacterEntityBuilder;
import com.yichi.onepiece.battle.model.CurrentCharacterEntityImpl;
import com.yichi.onepiece.character.model.CharacterEntity;
import com.yichi.onepiece.fruit.model.FruitEntity;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharacterInBattle {
  Logger logger = LoggerFactory.getLogger(CharacterInBattle.class);
  private static final int INIT_ENERGY_PERCENTAGE = 100;
  private final UUID characterId;
  private final String name;
  private int intelligence;
  private int magicConsume;
  private boolean defeated;
  private boolean smarter;
  private boolean reversed;
  private AttackEntity attackEntity;
  private final CharacterEntity baseEntity;
  private CurrentCharacterEntityImpl currentCharacterEntity;

  List<FruitEntity> fruitEntities;

  public CharacterInBattle(CharacterEntity baseEntity, List<FruitEntity> fruitEntities) {
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
    int magic = baseEntity.getMagic();
    int magicConsume = 0;
    // add fruit strengthen if this character eats fruits.
    if (baseEntity.getHasFruits() && !fruitEntities.isEmpty()) {
      magicConsume = fruitEntities.stream().mapToInt(FruitEntity::getMagicConsume).sum();
      for (FruitEntity fruit : fruitEntities) {
        blood = (int) (blood * fruit.getBloodStrengthen());
        attack = (int) (attack * fruit.getAttackStrengthen());
        intelligence = (int) (intelligence * fruit.getIntelligenceStrengthen());
        defense = (int) (defense * fruit.getDefenseStrengthen());
        speed = (int) (speed * fruit.getSpeedStrengthen());
      }
    }
    this.intelligence = intelligence;
    this.magicConsume = magicConsume;
    this.currentCharacterEntity =
        (CurrentCharacterEntityImpl)
            CurrentCharacterEntityBuilder.newBuilder()
                .setAttack(attack)
                .setBlood(blood)
                .setDefense(defense)
                .setMagic(magic)
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

  public int getMagic() {
    return currentCharacterEntity.getMagic();
  }

  public int getSpeed() {
    return currentCharacterEntity.getSpeed();
  }

  public String getName() {
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
    if (currentCharacterEntity.getMagic() < 0 && !reversed) {
      reverseCharacter();
      reversed = true;
      logger.info("run out of magic, reverse {}", name);
    }
    logger.info("{} attack with power {}", name, currentCharacterEntity.getMagic());
    if (baseEntity.getHasFruits() && !reversed) {
      currentCharacterEntity.setMagic(currentCharacterEntity.getMagic() - magicConsume);
    }
    return attackEntity;
  }

  // the order of return list is: blood loss, magic loss and energy loss.
  public List<Integer> defense(AttackEntity attackEntity) {
    if (currentCharacterEntity.getMagic() < 0 && !reversed) {
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
