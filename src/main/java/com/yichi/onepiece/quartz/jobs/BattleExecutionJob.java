package com.yichi.onepiece.quartz.jobs;

import com.yichi.onepiece.battle.service.BattleService;
import java.util.UUID;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import static com.yichi.onepiece.constants.ConstantsContainer.*;

@Component
public class BattleExecutionJob extends QuartzJobBean {
  private final Logger logger = LoggerFactory.getLogger(BattleExecutionJob.class);
  private final BattleService battleService;

  public BattleExecutionJob(BattleService battleService) {
    this.battleService = battleService;
  }


  @Override
  protected void executeInternal(@Nonnull JobExecutionContext jobExecutionContext) {
    try {
      String battleId = jobExecutionContext.getJobDetail().getJobDataMap().getString(BATTLE_ID);
      String userId = jobExecutionContext.getJobDetail().getJobDataMap().getString(USER_ID);
      String email = jobExecutionContext.getJobDetail().getJobDataMap().getString(USER_EMAIL);
      String nickname = jobExecutionContext.getJobDetail().getJobDataMap().getString(USER_NICKNAME);
      battleService.startBattle(UUID.fromString(battleId), UUID.fromString(userId), email, nickname);
    } catch (Exception e) {
      logger.error(e.toString());
    }
  }
}
