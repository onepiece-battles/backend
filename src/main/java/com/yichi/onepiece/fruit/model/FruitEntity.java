package com.yichi.onepiece.fruit.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.FruitType;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface FruitEntity {
  String ENTITY_TYPE = "FRUIT";

  UUID getId();

  String getName();

  @Nullable
  Byte[] getProfileImage();

  @Nullable
  UUID getOwnerId();

  FruitType getFruitType();

  // Paramecia fruits consume positive magic, other types consume 0 magic.
  Integer getMagicConsume();

  Double getGroupGainStrengthen();

  Double getAttackStrengthen();

  Double getDefenseStrengthen();

  Double getIntelligenceStrengthen();

  Double getBloodStrengthen();

  Double getSpeedStrengthen();

  Long getCreationRecordId();

  Long getModificationRecordId();

  FruitEntityBuilder toBuilder();
}
