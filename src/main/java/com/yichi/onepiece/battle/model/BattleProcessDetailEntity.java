package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface BattleProcessDetailEntity {
  UUID getId();
  UUID getBattleId();
  Integer getBattleProcessRoundNumber();
  Boolean getAttackMissed();
  Integer getAttackerMagicLoss();
  Integer getAttackerEnergyLoss();
  Integer getDefenderBloodLoss();
  Integer getDefenderEnergyLoss();
  Integer getDefenderMagicLoss();
  String getAttackSkillType();
  BattleProcessDetailEntityBuilder toBuilder();
}
