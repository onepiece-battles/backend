package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import java.util.List;

@Entity
public interface BattleFullView extends BattleListView {
  List<BattleProcessDetailEntity> getBattleProcessDetails();

  List<BattleStatusDetailEntity> getBattleStatusDetails();

  BattleListView getPreviousBattle();
  BattleListView getNextBattle();

  BattleFullViewBuilder toBuilder();
}
