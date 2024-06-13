package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface BattleProcessDetailEntity {
  UUID getId();
  UUID getBattleId();
  UUID getAttackerId();
  Integer getBattleProcessRoundNumber();
  Boolean getAttackMissed();
  Integer getAttackerEnergyLoss();
  Integer getDefenderBloodLoss();
  Integer getDefenderEnergyLoss();
  String getAttackSkill();
  BattleProcessDetailEntityBuilder toBuilder();
}
