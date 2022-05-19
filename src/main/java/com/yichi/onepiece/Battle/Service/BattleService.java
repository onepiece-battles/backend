package com.yichi.onepiece.Battle.Service;

import com.yichi.onepiece.Battle.Model.BattleDao;
import org.springframework.stereotype.Service;

@Service
public class BattleService {

  private final BattleDao battleDao;

  public BattleService(BattleDao battleDao) {
    this.battleDao = battleDao;
  }
}
