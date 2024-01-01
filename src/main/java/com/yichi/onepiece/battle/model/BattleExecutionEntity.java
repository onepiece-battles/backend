package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.BattleReportStage;
import com.yichi.onepiece.model.BattleStatus;
import java.util.UUID;

@Entity
public interface BattleExecutionEntity {
  Long getId();

  UUID getBattleId();

  BattleStatus getBattleStatus();

  BattleReportStage getBattleReportStage();

  Long getCreationTimestamp();

  Long getModificationTimestamp();
}
