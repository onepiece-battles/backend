package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.model.BattleReportStage;
import com.yichi.onepiece.model.BattleStatus;
import java.util.UUID;

@Entity
public interface BattleListView extends BattleEntity {

  CharacterEntity getStarter();

  CharacterEntity getReceiver();

  ActionRecordEntity getCreationRecord();

  ActionRecordEntity getModificationRecord();

  BattleExecutionEntity getBattleExecution();
}
