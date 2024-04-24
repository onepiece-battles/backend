package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.mybatis.annotations.Column;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.model.BattleStyle;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.Race;

import java.util.Date;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface CharacterEntity {
  String ENTITY_TYPE = "CHARACTER";

  UUID getId();

  @Column(json = true)
  I18nText getName();

  BattleStyle getBattleStyle();

  Race getRace();

  Integer getScore();

  @Nullable
  Byte[] getProfileImage();

  // default to be 1, unless special characters.
  Integer getMaximumFruitCount();

  Integer getAge();

  Double getHeight();

  String getDateOfBirth();

  String getBirthPlace();

  Integer getBounty();

  // some characters are better when fighting in a group
  Integer getBaseGroupGain();

  Integer getBaseAttack();

  Integer getBaseDefense();

  Integer getBaseIntelligence();

  Integer getBaseBlood();

  Integer getBaseSpeed();
  Integer getGroupGain();

  Integer getAttack();

  Integer getDefense();

  Integer getIntelligence();

  Integer getBlood();

  Integer getSpeed();

  Integer getEnergy();

  Boolean getSystemResource();

  Long getCreationRecordId();
  Long getModificationRecordId();

  CharacterEntityBuilder toBuilder();
}
