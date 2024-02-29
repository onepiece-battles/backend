package com.yichi.onepiece.quartz.jobs;

import com.yichi.onepiece.battle.service.BattleService;
import java.util.UUID;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import static com.yichi.onepiece.constants.ConstantsContainer.BATTLE_ID;

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
      battleService.startBattle(UUID.fromString(battleId));
    } catch (Exception e) {
      logger.error(e.toString());
    }
  }
}
