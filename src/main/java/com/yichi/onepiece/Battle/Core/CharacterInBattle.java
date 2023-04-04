package com.yichi.onepiece.Battle.Core;

import com.yichi.onepiece.Character.Model.CharacterEntity;
import com.yichi.onepiece.Fruits.Model.FruitEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class CharacterInBattle {
    Logger logger = LoggerFactory.getLogger(CharacterInBattle.class);
    private final UUID characterId;
    private final String name;
    private int blood;
    private int attack;
    private int speed;
    private int intelligence;
    private int defense;
    private int magicConsume;
    private boolean defeated;
    private boolean smarter;
    private boolean reversed;
    private int magic;
    AttackEntity attackEntity;
    CharacterEntity baseEntity;

    List<FruitEntity> fruitEntities;

    public CharacterInBattle(CharacterEntity baseEntity, List<FruitEntity> fruitEntities) {
        this.baseEntity = baseEntity;
        this.fruitEntities = fruitEntities;
        this.name = baseEntity.getName();
        this.defeated = false;
        this.reversed = false;
        this.smarter = false;
        this.characterId = baseEntity.getId();
        this.magic = baseEntity.getMagic();
        initWithFruit(baseEntity);
        initAttackEntity();
    }

    private void initWithFruit(CharacterEntity baseEntity) {
        int blood = baseEntity.getBlood();
        int attack = baseEntity.getAttack();
        int defense = baseEntity.getDefense();
        int intelligence = baseEntity.getIntelligence();
        int speed = baseEntity.getSpeed();
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
        this.blood = blood;
        this.attack = attack;
        this.intelligence = intelligence;
        this.speed = speed;
        this.defense = defense;
        this.magicConsume = magicConsume;
    }

    private void initAttackEntity() {
        this.attackEntity = AttackEntityBuilder.newBuilder().setAttack(attack).build();
    }

    public boolean getReversed() {
        return reversed;
    }

    public boolean getAlive() {
        return !defeated;
    }

    public int getBlood() {
        return blood;
    }

    public int getMagic() {
        return magic;
    }

    public int getSpeed() {
        return speed;
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
        if (magic < 0 && !reversed) {
            reverseCharacter();
            reversed = true;
            logger.info("run out of magic, reverse {}", name);
        }
        logger.info("{} attack with power {}", name, attack);
        if (baseEntity.getHasFruits() && !reversed) {
            magic = magic - magicConsume;
        }
        return attackEntity;
    }

    public void defense(AttackEntity attackEntity) {
        if (magic < 0 && !reversed) {
            reverseCharacter();
            reversed = true;
            logger.info("run out of magic, reverse {}", name);
        }

        if (blood <= 0) {
            this.defeated = true;
            return;
        }
        int bloodLoss = Math.max(attackEntity.getAttack() - defense, 0);
        blood = blood - bloodLoss;
        logger.info("{} defense with bloodLoss {}", name, bloodLoss);
    }

    private void reverseCharacter() {
        // blood strengthen can not be reversed.
        logger.info("{} is reversed, attack, defense, intelligence, speed are reset", name);
        this.attack = baseEntity.getAttack();
        this.defense = baseEntity.getDefense();
        this.intelligence = baseEntity.getIntelligence();
        this.speed = baseEntity.getSpeed();
        // refresh attackEntity
        attackEntity = attackEntity.toBuilder().setAttack(this.attack).build();
    }
}
