package com.yichi.onepiece.battle.async;

import com.yichi.onepiece.battle.service.BattleService;
import java.util.UUID;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class BattleExecutionJob implements Job {
  private final BattleService battleService;

  public BattleExecutionJob(BattleService battleService) {
    this.battleService = battleService;
  }

  @Override
  public void execute(JobExecutionContext context) {
    String battleId = context.getJobDetail().getJobDataMap().getString("battleId");
    battleService.startBattle(UUID.fromString(battleId));
  }
}
