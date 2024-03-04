package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.BattleStatus;
import com.yichi.onepiece.model.BattleType;
import java.util.UUID;

@Entity
public interface BattleEntity {
  String ENTITY_TYPE = "BATTLE";

  UUID getId();

  UUID getStarterId();

  UUID getReceiverId();

  UUID getPreviousBattleId();

  UUID getNextBattleId();

  Long getCreationRecordId();

  Long getModificationRecordId();

  BattleType getType();

  Boolean getSystemResource();

  BattleEntityBuilder toBuilder();
}
