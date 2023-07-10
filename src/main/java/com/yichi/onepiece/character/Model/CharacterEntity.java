package com.yichi.onepiece.character.Model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.CharacterType;
import com.yichi.onepiece.model.FruitInCharacter;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface CharacterEntity {
  String ENTITY_TYPE = "CHARACTER";

  UUID getId();

  CharacterType getCharacterType();

  @Nullable
  Byte[] getProfileImage();

  String getName();

  String getGroupOid();

  Boolean getHasFruits();

  // default to be 1, unless special characters.
  Integer getFruitLimit();

  List<FruitInCharacter> getFruits();

  // some characters are better when fighting in a group
  Integer getGroupGain();

  Integer getAttack();

  Integer getDefense();

  Integer getIntelligence();

  Integer getBlood();

  Integer getSpeed();

  // magic will be used if character has fruit in Paramecia type.
  Integer getMagic();

  CharacterEntityBuilder toBuilder();
}
