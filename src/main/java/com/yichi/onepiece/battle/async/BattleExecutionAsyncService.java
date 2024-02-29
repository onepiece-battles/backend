package com.yichi.onepiece.battle.async;

import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.BattleStatus;
import com.yichi.onepiece.quartz.jobs.BattleExecutionJob;
import com.yichi.onepiece.quartz.jobs.EmailJob;
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
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.yichi.onepiece.constants.ConstantsContainer.*;
import static com.yichi.onepiece.constants.ConstantsContainer.EMAIL_JOB_GROUP;

@Service
public class BattleExecutionAsyncService {
  private final Logger logger = LoggerFactory.getLogger(BattleExecutionAsyncService.class);
  private final BattleService battleService;
  private final ActionRecordService actionRecordService;
  private final SchedulerFactoryBean schedulerFactoryBean;


  public BattleExecutionAsyncService(
          BattleService battleService, ActionRecordService actionRecordService, SchedulerFactoryBean schedulerFactoryBean) {
    this.battleService = battleService;
    this.actionRecordService = actionRecordService;
    this.schedulerFactoryBean = schedulerFactoryBean;
  }

  @Timed
  @Transactional(propagation = Propagation.MANDATORY)
  public void scheduleAsyncJob(UUID battleId) {
    var battleExecution = battleService.getBattleExecutionStatusByBattleId(battleId);
    battleService.checkBattleStatus(battleExecution.getBattleStatus());
    var actionRecord = actionRecordService.create();
    battleExecution = battleExecution.toBuilder().setModificationTimestamp(
        actionRecord.getActionTimestamp()).setBattleStatus(BattleStatus.IN_PROGRESS).build();
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put(BATTLE_ID, battleId.toString());
    JobDetail battleStartJob = JobBuilder.newJob(BattleExecutionJob.class)
            .setJobData(jobDataMap)
            .withIdentity(UUID.randomUUID().toString(), BATTLE_JOB_GROUP)
            .withDescription("start battle job")
            .build();
    Scheduler scheduler = schedulerFactoryBean.getScheduler();
    Trigger trigger = TriggerBuilder.newTrigger()
            .forJob(battleStartJob)
            .startNow().build();
    try{
      scheduler.start();
      scheduler.scheduleJob(battleStartJob, trigger);
    } catch (Exception e) {
      logger.error(e.toString());
    }
    battleService.updateBattleExecutionEntity(battleExecution);
  }
}
