package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface BattleStatusDetailEntity {
  UUID getId();
  UUID getBattleId();
  Integer getBattleStatusRoundNumber();
  String getAttackSide();
  CurrentCharacterEntity getAttacker();
  CurrentCharacterEntity getDefender();
  BattleStatusDetailEntityBuilder toBuilder();
}
