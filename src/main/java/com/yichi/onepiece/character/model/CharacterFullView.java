package com.yichi.onepiece.character.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.battle.model.BattleFullView;
import java.util.List;

@Entity
public interface CharacterFullView extends CharacterEntity {
  List<BattleFullView> getBattleIds();
}
