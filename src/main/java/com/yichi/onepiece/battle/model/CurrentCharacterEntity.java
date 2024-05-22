package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;

@Entity
public interface CurrentCharacterEntity {
  Integer getBlood();

  Integer getCurrentEnergy();

  Integer getCurrentEnergyConsume();

  Integer getAttack();

  Integer getIntelligence();

  Integer getDefense();

  Integer getSpeed();

  CurrentCharacterEntityBuilder toBuilder();
}
