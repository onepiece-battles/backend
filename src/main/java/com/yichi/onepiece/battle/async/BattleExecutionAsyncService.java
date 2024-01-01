package com.yichi.onepiece.battle.async;

import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

  public void scheduleAsyncJob(UUID battleId) {
    var battle = battleService.findBattleById(battleId);
    battleService.checkBattleStatus(battle.getStatus());
    var actionRecord = actionRecordService.create();
  }
}
