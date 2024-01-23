package com.yichi.onepiece.battle.async;

import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.BattleStatus;
import io.micrometer.core.annotation.Timed;
import java.util.UUID;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BattleExecutionAsyncService {
  private final Logger logger = LoggerFactory.getLogger(BattleExecutionAsyncService.class);
  private final BattleService battleService;
  private final ActionRecordService actionRecordService;

  public BattleExecutionAsyncService(
      BattleService battleService, ActionRecordService actionRecordService) {
    this.battleService = battleService;
    this.actionRecordService = actionRecordService;
  }

  @Timed
  @Transactional(propagation = Propagation.MANDATORY)
  public void scheduleAsyncJob(UUID battleId) {
    var battleExecution = battleService.getBattleExecutionStatusByBattleId(battleId);
    battleService.checkBattleStatus(battleExecution.getBattleStatus());
    var actionRecord = actionRecordService.create();
    battleExecution = battleExecution.toBuilder().setModificationTimestamp(
        actionRecord.getActionTimestamp()).setBattleStatus(BattleStatus.IN_PROGRESS).build();

    battleService.updateBattleExecutionEntity(battleExecution);
  }
}
