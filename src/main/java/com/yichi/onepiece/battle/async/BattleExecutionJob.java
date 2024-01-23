package com.yichi.onepiece.battle.async;

import com.yichi.onepiece.battle.service.BattleService;
import java.util.UUID;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

@Component
public class BattleExecutionJob extends QuartzJobBean {
  private final BattleService battleService;

  public BattleExecutionJob(BattleService battleService) {
    this.battleService = battleService;
  }


  @Override
  protected void executeInternal(JobExecutionContext jobExecutionContext) {
    try {
      String battleId = jobExecutionContext.getJobDetail().getJobDataMap().getString("battleId");
      battleService.startBattle(UUID.fromString(battleId));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
