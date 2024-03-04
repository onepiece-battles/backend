package com.yichi.onepiece.group.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.entity.CharacterInGroupView;

import java.util.List;
import java.util.UUID;

@Entity
public interface GroupFullView extends GroupListView {

  GroupEntity getNextGroup();
  GroupEntity getPreviousGroup();
  List<CharacterInGroupView> getCharacters();
  GroupFullViewBuilder toBuilder();
}
