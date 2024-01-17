package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.mybatis.annotations.Column;
import com.yichi.onepiece.model.CharacterType;
import com.yichi.onepiece.model.Haki;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.UserRole;
import java.util.List;
import javax.annotation.Nullable;

@Entity
public interface CharacterListView extends CharacterEntity {
  Long getCreationTimestamp();

  Long getModificationTimestamp();

  UserRole getRoleName();

  String getCreator();

  List<CharacterType> getCharacterTypes();

  List<Haki> getHakis();
}
