package com.yichi.onepiece.battle.controller;

import com.yichi.onepiece.api.BattleApi;
import com.yichi.onepiece.battle.model.BattleEntity;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.iam.IamService;
import com.yichi.onepiece.iam.PermissionDefinition.PermissionEnum;
import com.yichi.onepiece.model.AddBattleRequest;
import com.yichi.onepiece.model.Battle;
import com.yichi.onepiece.model.StartBattleRequest;
import com.yichi.onepiece.model.StartBattleResponse;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class BattleController implements BattleApi {
  private final BattleService battleService;
  private final IamService iamService;

  public BattleController(BattleService battleService, IamService iamService) {
    this.battleService = battleService;
    this.iamService = iamService;
  }

  @Override
  public ResponseEntity<Battle> addBattle(AddBattleRequest addBattleRequest) {
    iamService.checkPermissionOfCurrentAccount(
        BattleEntity.ENTITY_TYPE, Set.of(PermissionEnum.BATTLE_CREATE, PermissionEnum.BATTLE_GET));
    var battle = battleService.addBattle(addBattleRequest);
    return ResponseEntity.ok(battle);
  }

  @Override
  public ResponseEntity<StartBattleResponse> startBattle(StartBattleRequest startBattleRequest) {
    iamService.checkPermissionOfCurrentAccount(
        BattleEntity.ENTITY_TYPE, Set.of(PermissionEnum.BATTLE_UPDATE));
    var battle = battleService.startBattle(startBattleRequest.getBattleId());
    return ResponseEntity.ok(battle);
  }
}
