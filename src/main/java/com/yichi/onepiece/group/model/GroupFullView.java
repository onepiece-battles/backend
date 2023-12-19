package com.yichi.onepiece.group.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.CharacterInGroupView;
import java.util.List;

@Entity
public interface GroupFullView extends GroupEntity {
  List<CharacterInGroupView> getCharacters();
}
