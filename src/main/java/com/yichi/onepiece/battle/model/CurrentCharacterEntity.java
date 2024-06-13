package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;

import java.util.UUID;

@Entity
public interface CurrentCharacterEntity {
  UUID getCharacterId();
  Integer getBlood();

  Integer getCurrentEnergy();

  Integer getCurrentEnergyConsume();

  Integer getAttack();

  Integer getIntelligence();

  Integer getDefense();

  Integer getSpeed();

  CurrentCharacterEntityBuilder toBuilder();
}
