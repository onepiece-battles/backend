package com.yichi.onepiece.battle.controller;

import com.yichi.onepiece.api.BattleApi;
import com.yichi.onepiece.battle.async.BattleExecutionAsyncService;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

@Controller
public class BattleController implements BattleApi {
  Logger logger = LoggerFactory.getLogger(BattleController.class);

  private final BattleService battleService;
  private final BattleExecutionAsyncService battleExecutionAsyncService;
  private final IamService iamService;

  public BattleController(
      BattleService battleService,
      BattleExecutionAsyncService battleExecutionAsyncService,
      IamService iamService) {
    this.battleService = battleService;
    this.battleExecutionAsyncService = battleExecutionAsyncService;
    this.iamService = iamService;
  }

  @Override
  public ResponseEntity<Battle> addBattle(AddBattleRequest addBattleRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.BATTLE, Set.of(Permission.CREATE, Permission.GET));
    var battle = battleService.createBattle(addBattleRequest);
    return ResponseEntity.ok(battle);
  }

  @Override
  public ResponseEntity<BattleFullView> getBattleFullView(
      GetBattleFullViewRequest getBattleFullViewRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.BATTLE, Set.of(Permission.GET));
    StopWatch watch = new StopWatch();
    watch.start();
    var response = battleService.getBattleFullView(getBattleFullViewRequest);
    watch.stop();
    logger.info("get battle takes: {}", watch.getTotalTimeSeconds());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<BattleExecution> getBattleExecution(
      GetBattleExecutionRequest getBattleLiteRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.BATTLE, Set.of(Permission.GET));
    return ResponseEntity.ok(battleService.getBattleExecution(getBattleLiteRequest));
  }

  @Override
  public ResponseEntity<ListBattleByIdsResponse> listBattleByIds(
      ListBattleByIdsRequest listBattleByIdsRequest) {
    StopWatch watch = new StopWatch();
    watch.start();
    iamService.checkPermissionOfCurrentAccount(ResourceType.BATTLE, Set.of(Permission.LIST));
    var response = battleService.batchFindById(listBattleByIdsRequest);
    watch.stop();
    logger.info("list battle by ids takes: {}", watch.getTotalTimeSeconds());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ListBattleResponse> listBattles(ListBattleRequest listBattleRequest) {
    StopWatch watch = new StopWatch();
    watch.start();
    iamService.checkPermissionOfCurrentAccount(ResourceType.BATTLE, Set.of(Permission.LIST));
    logger.info("starter name {}, receiver name {}", listBattleRequest.getBattleFilter().getStarterName(),  listBattleRequest.getBattleFilter().getReceiverName());
    var response = battleService.list(listBattleRequest);
    watch.stop();
    logger.info("list battle takes: {}", watch.getTotalTimeSeconds());
    return ResponseEntity.ok(response);
  }

  @Override
  @Transactional
  public ResponseEntity<StartBattleResponse> startBattle(StartBattleRequest startBattleRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.BATTLE, Set.of(Permission.UPDATE));
    battleExecutionAsyncService.scheduleAsyncJob(startBattleRequest.getBattleId());
    // update start battle response api
    return ResponseEntity.ok(new StartBattleResponse());
  }
}
