package com.yichi.onepiece.group.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.UserRole;
import java.util.List;

@Entity
public interface GroupListView {
  Long getCreationTimestamp();
  Long getModificationTimestamp();
  UserRole getRoleName();
  List<CharacterInGroupLite> getCharacters();
  String getCreator();
}
