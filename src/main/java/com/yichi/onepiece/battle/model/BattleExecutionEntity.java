package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.mybatis.annotations.Column;
import com.yichi.onepiece.model.BattleReportStage;
import com.yichi.onepiece.model.BattleStatus;
import java.util.UUID;

@Entity
public interface BattleExecutionEntity {
  UUID getId();

  @Column(ignoreUpdates = true)
  UUID getBattleId();

  BattleStatus getBattleStatus();

  BattleReportStage getBattleReportStage();

  UUID getReportFileId();

  UUID getWinnerId();

  Long getCreationTimestamp();

  Long getModificationTimestamp();

  BattleExecutionEntityBuilder toBuilder();
}
