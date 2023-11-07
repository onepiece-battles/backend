package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.CharacterEntity;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.user.model.UserEntity;

@Entity
public interface BattleFullViewEntity extends BattleEntity {
  CharacterEntity getStarter();

  CharacterEntity getReceiver();

  ActionRecordEntity getCreationRecord();

  ActionRecordEntity getModificationRecord();

  UserEntity getUser();
}
