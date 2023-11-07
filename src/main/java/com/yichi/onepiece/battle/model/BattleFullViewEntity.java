package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.CharacterEntity;

@Entity
public interface BattleFullViewEntity extends BattleEntity{
  CharacterEntity getStarter();
  CharacterEntity getReceiver();
}
