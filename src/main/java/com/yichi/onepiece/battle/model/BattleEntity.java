package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.BattleType;
import java.util.UUID;

@Entity
public interface BattleEntity {
  String ENTITY_TYPE = "BATTLE";

  UUID getId();

  UUID getUserId();

  UUID getCharacterIdInitiator();

  UUID getCharacterIdReceiver();

  BattleType getBattleType();

  Long getCreationRecordId();

  Long getModificationRecordId();
}
