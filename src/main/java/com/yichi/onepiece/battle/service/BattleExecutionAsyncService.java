package com.yichi.onepiece.battle.service;

import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.BattleStatus;
import com.yichi.onepiece.quartz.jobs.BattleExecutionJob;
import com.yichi.onepiece.user.UserHelper;
import io.micrometer.core.annotation.Timed;
import java.util.UUID;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.yichi.onepiece.constants.OnepieceConstants.*;

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
    jobDataMap.put(USER_ID, UserHelper.getCurrentUserId().toString());
    jobDataMap.put(USER_EMAIL, UserHelper.getCurrentUserDetail().getUsername());
    jobDataMap.put(USER_NICKNAME, UserHelper.getCurrentUserDetail().getNickname());
    jobDataMap.put(ROLE_NAME, UserHelper.getCurrentUserDetail().getLoginRole().toString());
    jobDataMap.put(LOCALE, UserHelper.getCurrentUserDetail().getLocale().toString());
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
      throw new IllegalStateException("Could not schedule battle execution", e);
    }
    battleService.updateBattleExecutionEntity(battleExecution);
  }
}
