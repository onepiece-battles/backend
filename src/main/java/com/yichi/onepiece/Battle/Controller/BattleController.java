package com.yichi.onepiece.Battle.Controller;

import com.yichi.onepiece.Account.Service.AccountService;
import com.yichi.onepiece.Battle.Service.BattleService;
import com.yichi.onepiece.Character.Service.CharacterService;
import com.yichi.onepiece.api.BattleApi;
import com.yichi.onepiece.model.AddBattleRequest;
import com.yichi.onepiece.model.Battle;
import com.yichi.onepiece.model.StartBattleRequest;
import com.yichi.onepiece.model.StartBattleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class BattleController implements BattleApi {

  private final BattleService battleService;
  private final AccountService accountService;

  private final CharacterService characterService;

  public BattleController(
      BattleService battleService,
      AccountService accountService,
      CharacterService characterService) {
    this.battleService = battleService;
    this.accountService = accountService;
    this.characterService = characterService;
  }

  @Override
  public ResponseEntity<Battle> addBattle(AddBattleRequest addBattleRequest) {
    accountService.justifyUser(addBattleRequest.getUserId());
    var battle = battleService.addBattle(addBattleRequest);
    return ResponseEntity.ok(battle);
  }

  @Override
  public ResponseEntity<StartBattleResponse> startBattle(StartBattleRequest startBattleRequest) {
    var battle = battleService.startBattle(startBattleRequest.getBattleId());
    return ResponseEntity.ok(battle);
  }
}
