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
  private final int score;

  // energy consume for character's ordinary attack without any fruit.
  private static final int DEFAULT_ENERGY_CONSUME = 2;
  private static final int DEFENDING_ENERGY_CONSUME = 2;
  private int minimumFruitEnergy;
  private final UUID characterId;
  private final I18nText name;
  private boolean defeated;
  private boolean reversed;
  private final CharacterEntity baseEntity;
  private CurrentCharacterEntityImpl currentCharacter;

  List<FruitEntity> fruitEntities;

  public CharacterInBattle(CharacterEntity baseEntity, List<FruitEntity> fruitEntities) {
    this.baseEntity = baseEntity;
    this.fruitEntities = fruitEntities;
    this.name = baseEntity.getName();
    this.defeated = false;
    this.reversed = false;
    this.characterId = baseEntity.getId();
    this.score = baseEntity.getScore();
    initCurrentCharacter(baseEntity);
  }

  private void initCurrentCharacter(CharacterEntity baseEntity) {
    minimumFruitEnergy = baseEntity.getEnergy()/10;
    this.currentCharacter =
            (CurrentCharacterEntityImpl)
                    CurrentCharacterEntityBuilder.newBuilder()
                            .setAttack(baseEntity.getAttack())
                            .setBlood(baseEntity.getBlood())
                            .setDefense(baseEntity.getDefense())
                            .setIntelligence(baseEntity.getIntelligence())
                            .setCurrentEnergy(baseEntity.getEnergy())
                            .setCurrentEnergyConsume(
                                    DEFAULT_ENERGY_CONSUME +
                                            fruitEntities.stream().mapToInt(FruitEntity::getEnergyConsume).sum())
                            .setSpeed(baseEntity.getSpeed())
                            .build();
  }
  public boolean getReversed() {
    return reversed;
  }
  public boolean isAlive() {
    if (currentCharacter.getCurrentEnergy()<=0 || currentCharacter.getBlood()<=0 ) {
      defeated = true;
    }
    return !defeated;
  }

  public int getScore() {return score;}
  public I18nText getName() {
    return name;
  }

  public int getBlood() {
    return currentCharacter.getBlood();
  }

  public int getSpeed() {
    return currentCharacter.getSpeed();
  }

  public int getEnergyConsume() {
    return currentCharacter.getCurrentEnergyConsume();
  }

  public UUID getCharacterId() {
    return characterId;
  }

  public int getIntelligence() {
    return currentCharacter.getIntelligence();
  }

  public AttackEntity attack() {
    if (currentCharacter.getCurrentEnergy() < minimumFruitEnergy && !reversed) {
      reverseCharacter();
      reversed = true;
    }
    currentCharacter.setCurrentEnergy(currentCharacter.getCurrentEnergy()-currentCharacter.getCurrentEnergyConsume());
    return AttackEntityBuilder.newBuilder()
            .setAttack(currentCharacter.getAttack()).build();
  }

  // the order of return list is: blood loss, and energy loss.
  public List<Integer> defend(AttackEntity attackEntity) {
    int blood = currentCharacter.getBlood();
    if (blood <= 0) {
      this.defeated = true;
      return List.of(0, 0);
    }
    int bloodLoss = Math.max(attackEntity.getAttack() - currentCharacter.getDefense(), 0);
    currentCharacter.setBlood(currentCharacter.getBlood() - bloodLoss);
    currentCharacter.setCurrentEnergy(currentCharacter.getCurrentEnergy() - DEFENDING_ENERGY_CONSUME);
    return List.of(bloodLoss, DEFENDING_ENERGY_CONSUME);
  }

  public CurrentCharacterEntityImpl getCurrentCharacterEntity() {
    return currentCharacter;
  }

  private void reverseCharacter() {
    currentCharacter = (CurrentCharacterEntityImpl)
            CurrentCharacterEntityBuilder.newBuilder()
                    .setAttack(baseEntity.getBaseAttack())
                    .setBlood(currentCharacter.getBlood())
                    .setDefense(baseEntity.getBaseDefense())
                    .setIntelligence(baseEntity.getBaseIntelligence())
                    .setCurrentEnergy(currentCharacter.getCurrentEnergy())
                    .setCurrentEnergyConsume(DEFAULT_ENERGY_CONSUME)
                    .setSpeed(baseEntity.getBaseSpeed())
                    .build();
  }
}
