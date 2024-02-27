package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;

@Entity
public interface BattleListView extends BattleEntity {

  CharacterEntity getStarter();

  CharacterEntity getReceiver();

  ActionRecordEntity getCreationRecord();

  ActionRecordEntity getModificationRecord();

  BattleExecutionEntity getBattleExecution();
}
