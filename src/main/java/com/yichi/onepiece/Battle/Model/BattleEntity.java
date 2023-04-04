package com.yichi.onepiece.Battle.Model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.BattleType;

import java.util.UUID;

@Entity
public interface BattleEntity {
    UUID getId();

    UUID getUserId();

    UUID getCharacterIdInitiator();

    UUID getCharacterIdReceiver();

    BattleType getBattleType();

    Long getActionTimestamp();
}
