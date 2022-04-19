package com.yichi.onepiece.Fruits.Model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.FruitType;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface FruitEntity {
  UUID getId();

  String getName();

  @Nullable
  UUID getOwnerId();

  FruitType getFruitType();

  // Paramecia fruits consume positive magic, other types consume 0 magic.
  Integer getMagicConsume();

  Integer getGroupGainStrengthen();

  Integer getAttackStrengthen();

  Integer getDefenseStrengthen();

  Integer getIntelligenceStrengthen();

  Integer getBloodStrengthen();

  FruitEntityBuilder toBuilder();
}
