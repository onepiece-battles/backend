package com.yichi.onepiece.group.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.entity.CharacterInGroupLite;
import com.yichi.onepiece.model.UserRole;
import java.util.List;

@Entity
public interface GroupListView extends GroupEntity {
  Long getCreationTimestamp();

  Long getModificationTimestamp();

  UserRole getRoleName();

  List<CharacterInGroupLite> getCharacters();

  String getCreator();
}
