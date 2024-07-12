package com.yichi.onepiece.fruit.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.entity.CharacterInFruitView;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.model.UserRole;

import java.util.List;

@Entity
public interface FruitListView extends FruitEntity {
  ActionRecordEntity getCreationRecord();
  List<CharacterInFruitView> getCharacters();
  ActionRecordEntity getModificationRecord();
}
