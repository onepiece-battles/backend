package com.yichi.onepiece.character.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.UserRole;

@Entity
public interface CharacterListView extends CharacterEntity {
  Long getCreationTimestamp();
  Long getModificationTimestamp();
  UserRole getRoleName();
  String getCreator();
}
