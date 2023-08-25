package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;

@Entity
public interface CurrentCharacterEntity {
  Integer getBlood();

  Integer getMagic();

  Integer getEnergyPercentage();

  Integer getAttack();

  Integer getDefense();

  Integer getSpeed();

  CurrentCharacterEntityBuilder toBuilder();
}
