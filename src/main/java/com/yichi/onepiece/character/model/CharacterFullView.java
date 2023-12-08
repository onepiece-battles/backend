package com.yichi.onepiece.character.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.battle.model.BattleListView;
import com.yichi.onepiece.fruit.model.FruitListView;
import com.yichi.onepiece.group.model.GroupEntity;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import java.util.List;

@Entity
public interface CharacterFullView extends CharacterEntity {
  List<BattleListView> getBattles();
  List<FruitListView> getFruits();
  List<CharacterToFruitEntity> getFruitInCharacter();
  List<GroupEntity> getGroups();
  ActionRecordEntity getCreationRecord();
  ActionRecordEntity getModificationRecord();
}
