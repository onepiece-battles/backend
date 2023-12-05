package com.yichi.onepiece.battle.controller;

import com.yichi.onepiece.api.BattleApi;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.AddBattleRequest;
import com.yichi.onepiece.model.Battle;
import com.yichi.onepiece.model.BattleFullView;
import com.yichi.onepiece.model.GetBattleFullViewRequest;
import com.yichi.onepiece.model.ListBattleByIdResponse;
import com.yichi.onepiece.model.ListBattleByIdsRequest;
import com.yichi.onepiece.model.ListBattleRequest;
import com.yichi.onepiece.model.ListBattleResponse;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.StartBattleRequest;
import com.yichi.onepiece.model.StartBattleResponse;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StopWatch;

@Controller
public class BattleController implements BattleApi {
  Logger logger = LoggerFactory.getLogger(BattleController.class);

  private final BattleService battleService;
  private final IamService iamService;

  public BattleController(BattleService battleService, IamService iamService) {
    this.battleService = battleService;
    this.iamService = iamService;
  }

  @Override
  public ResponseEntity<Battle> addBattle(AddBattleRequest addBattleRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.BATTLE, Set.of(Permission.CREATE, Permission.GET));
    var battle = battleService.addBattle(addBattleRequest);
    return ResponseEntity.ok(battle);
  }

  @Override
  public ResponseEntity<BattleFullView> getBattleFullView(
      GetBattleFullViewRequest getBattleFullViewRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.BATTLE, Set.of(Permission.GET));
    StopWatch watch = new StopWatch();
    watch.start();
    var response = battleService.getBattleFullView(getBattleFullViewRequest);
    watch.stop();
    logger.info("get battle takes: {}", watch.getTotalTimeSeconds());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ListBattleByIdResponse> listBattleByIds(
      ListBattleByIdsRequest listBattleByIdsRequest) {
    StopWatch watch = new StopWatch();
    watch.start();
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.BATTLE, Set.of(Permission.LIST));
    var response = battleService.batchFindById(listBattleByIdsRequest);
    watch.stop();
    logger.info("list battle by ids takes: {}", watch.getTotalTimeSeconds());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ListBattleResponse> listBattles(ListBattleRequest listBattleRequest) {
    StopWatch watch = new StopWatch();
    watch.start();
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.BATTLE, Set.of(Permission.LIST));
    var response = battleService.list(listBattleRequest);
    watch.stop();
    logger.info("list battle takes: {}", watch.getTotalTimeSeconds());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<StartBattleResponse> startBattle(StartBattleRequest startBattleRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.BATTLE, Set.of(Permission.UPDATE));
    var battle = battleService.startBattle(startBattleRequest.getBattleId());
    return ResponseEntity.ok(battle);
  }
}
