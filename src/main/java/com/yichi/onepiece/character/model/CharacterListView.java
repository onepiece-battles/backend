package com.yichi.onepiece.character.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.CharacterType;
import com.yichi.onepiece.model.Haki;
import com.yichi.onepiece.model.UserRole;
import java.util.List;

@Entity
public interface CharacterListView extends CharacterEntity {
  Long getCreationTimestamp();

  Long getModificationTimestamp();

  String getGroupName();
  String getGroupPosition();

  UserRole getRoleName();

  String getCreator();

  List<CharacterType> getCharacterTypes();

  List<Haki> getHakis();
}
