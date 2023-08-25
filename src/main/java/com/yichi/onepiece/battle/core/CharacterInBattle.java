package com.yichi.onepiece.battle.core;

import com.yichi.onepiece.battle.model.CurrentCharacter;
import com.yichi.onepiece.character.model.CharacterEntity;
import com.yichi.onepiece.fruit.model.FruitEntity;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CharacterInBattle {
  Logger logger = LoggerFactory.getLogger(CharacterInBattle.class);
  private final static int INIT_ENERGY_PERCENTAGE = 100;
  private final UUID characterId;
  private final String name;
  private int intelligence;
  private int magicConsume;
  private boolean defeated;
  private boolean smarter;
  private boolean reversed;
  private AttackEntity attackEntity;
  private final CharacterEntity baseEntity;
  private CurrentCharacter currentCharacter = new CurrentCharacter();



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
    if (baseEntity.getHasFruits() && baseEntity.getFruits() != null) {
      magicConsume = fruitEntities.stream().mapToInt(FruitEntity::getMagicConsume).sum();
      for (FruitEntity fruit : fruitEntities) {
        // todo: update fruit table schema
        blood = (int) (blood * fruit.getBloodStrengthen());
        attack = (int) (attack * fruit.getAttackStrengthen());
        intelligence = (int) (intelligence * fruit.getIntelligenceStrengthen());
        defense = (int) (defense * fruit.getDefenseStrengthen());
        speed = (int) (speed * fruit.getSpeedStrengthen());
      }
    }
    this.intelligence = intelligence;
    this.magicConsume = magicConsume;
    this.currentCharacter = new CurrentCharacter(blood,magic, INIT_ENERGY_PERCENTAGE,attack,speed,defense);
  }

  private void initAttackEntity() {
    this.attackEntity =
        AttackEntityBuilder.newBuilder().setAttack(currentCharacter.getAttack()).build();
  }

  public boolean getReversed() {
    return reversed;
  }

  public boolean isAlive() {
    return !defeated;
  }

  public int getBlood() {
    return currentCharacter.getBlood();
  }

  public int getMagic() {
    return currentCharacter.getMagic();
  }

  public int getSpeed() {
    return currentCharacter.getSpeed();
  }

  public String getName() {
    return name;
  }

  public boolean getSmarter() {
    return smarter;
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
    if (currentCharacter.getMagic() < 0 && !reversed) {
      reverseCharacter();
      reversed = true;
      logger.info("run out of magic, reverse {}", name);
    }
    logger.info("{} attack with power {}", name, currentCharacter.getMagic());
    if (baseEntity.getHasFruits() && !reversed) {
      currentCharacter.setMagic(currentCharacter.getMagic()-magicConsume);
    }
    return attackEntity;
  }

  public void defense(AttackEntity attackEntity) {
    if (currentCharacter.getMagic() < 0 && !reversed) {
      reverseCharacter();
      reversed = true;
      logger.info("run out of magic, reverse {}", name);
    }
    int blood = currentCharacter.getBlood();
    if (blood <= 0) {
      this.defeated = true;
      return;
    }
    int bloodLoss = Math.max(attackEntity.getAttack() - currentCharacter.getDefense(), 0);
    currentCharacter.setBlood(blood - bloodLoss);
    logger.info("{} defense with bloodLoss {}", name, bloodLoss);
  }

  private void reverseCharacter() {
    // blood strengthen can not be reversed.
    logger.info("{} is reversed, attack, defense, intelligence, speed are reset", name);
    currentCharacter.setAttack(baseEntity.getAttack());
    currentCharacter.setDefense(baseEntity.getDefense());
    currentCharacter.setSpeed(baseEntity.getSpeed());
    this.intelligence = baseEntity.getIntelligence();
    // refresh attackEntity
    attackEntity = attackEntity.toBuilder().setAttack(currentCharacter.getAttack()).build();
  }
}
