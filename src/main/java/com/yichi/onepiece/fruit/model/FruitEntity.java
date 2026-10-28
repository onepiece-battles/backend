package com.yichi.onepiece.fruit.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.mybatis.annotations.Column;
import com.yichi.onepiece.model.FruitType;
import com.yichi.onepiece.model.I18nText;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface FruitEntity {
  String ENTITY_TYPE = "FRUIT";

  UUID getId();

  @Column(json = true)
  I18nText getName();

  String getImageUri();

  FruitType getFruitType();

  // Paramecia fruits consume positive extra energy and other fruits consumes no extra energy
  Integer getEnergyConsume();

  Double getGroupGainSlope();

  Integer getGroupGainIntercept();

  Double getAttackSlope();

  Integer getAttackIntercept();

  Double getDefenseSlope();
  Integer getDefenseIntercept();

  Double getIntelligenceSlope();
  Integer getIntelligenceIntercept();

  Double getBloodSlope();
  Integer getBloodIntercept();

  Double getSpeedSlope();
  Integer getSpeedIntercept();

  Long getCreationRecordId();

  Long getModificationRecordId();

  Boolean getSystemResource();

  FruitEntityBuilder toBuilder();
}
