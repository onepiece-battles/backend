package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.mybatis.annotations.Column;
import com.yichi.onepiece.model.I18nText;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface CharacterEntity {
  String ENTITY_TYPE = "CHARACTER";

  UUID getId();

  UUID getUserId();

  @Column(json = true)
  I18nText getName();

  @Nullable
  Byte[] getProfileImage();

  // default to be 1, unless special characters.
  Integer getMaximumFruitCount();

  Integer getAge();

  Double getHeight();

  Date getDateOfBirth();

  String getBirthPlace();

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

  Boolean getSystemResource();

  CharacterEntityBuilder toBuilder();
}
