package com.yichi.onepiece.Fruits.Model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.FruitType;

import javax.annotation.Nullable;
import java.util.UUID;

@Entity
public interface FruitEntity {
    UUID getId();

    String getName();

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

    FruitEntityBuilder toBuilder();
}
