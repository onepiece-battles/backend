package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import java.util.List;

@Entity
public interface BattleFullView extends BattleEntity {
  CharacterEntity getStarter();

  CharacterEntity getReceiver();

  List<BattleProcessDetailEntity> getBattleProcessDetails();

  List<BattleStatusDetailEntity> getBattleStatusDetails();

  ActionRecordEntity getCreationRecord();

  ActionRecordEntity getModificationRecord();
}
