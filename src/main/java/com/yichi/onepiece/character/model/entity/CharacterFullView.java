package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.battle.model.BattleListView;
import com.yichi.onepiece.fruit.model.FruitListView;
import com.yichi.onepiece.group.model.GroupEntity;
import java.util.List;
import java.util.UUID;

@Entity
public interface CharacterFullView extends CharacterListView {
  CharacterEntity getPreviousCharacter();
  CharacterEntity getNextCharacter();
  List<BattleListView> getBattles();

  List<FruitListView> getFruits();

  List<CharacterToFruitEntity> getFruitInCharacter();

  List<GroupForCharacterView> getGroups();

  CharacterFullViewBuilder toBuilder();
}
