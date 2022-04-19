package com.yichi.onepiece.Character.Model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.CharacterTypeInfo;
import com.yichi.onepiece.model.FruitInCharacter;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface CharacterBookEntity {
  UUID getId();

  List<CharacterTypeInfo> getCharacterTypes();

  @Nullable
  Byte[] getProfileImage();

  String getName();

  String getGroupOid();

  Boolean getHasFruits();

  // default to be 1, unless special characters.
  Integer getFruitLimit();

  @Nullable
  List<FruitInCharacter> getFruits();
  // some characters are better when fighting in a group
  Integer getGroupGain();

  Integer getAttack();

  Integer getDefense();

  Integer getIntelligence();

  Integer getBlood();

  // magic will be used if character has fruit in Paramecia type.
  Integer getMagic();

  CharacterBookEntityBuilder toBuilder();
}
