package com.yichi.onepiece.character.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.CharacterType;
import com.yichi.onepiece.model.Haki;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface CharacterEntity {
  String ENTITY_TYPE = "CHARACTER";

  UUID getId();

  UUID getUserId();
  UUID getRoleId();
  String getName();

  List<CharacterType> getCharacterTypes();

  @Nullable
  Byte[] getProfileImage();
  UUID getGroupId();
  // default to be 1, unless special characters.
  Integer getMaximumFruitCount();
  Integer getAge();
  Double getHeight();
  Date getDateOfBirth();
  List<Haki> getHakis();
  Integer getBounty();

  // some characters are better when fighting in a group
  Integer getGroupGain();

  Integer getAttack();

  Integer getDefense();

  Integer getIntelligence();

  Integer getBlood();

  Integer getSpeed();

  // magic will be used if character has fruit in Paramecia type.
  Integer getEnergy();

  Long getCreationRecordId();

  Long getModificationRecordId();

  CharacterEntityBuilder toBuilder();
}
