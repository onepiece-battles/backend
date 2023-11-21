package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.BattleStatus;
import com.yichi.onepiece.model.BattleType;
import java.util.UUID;

@Entity
public interface BattleEntity {
  String ENTITY_TYPE = "BATTLE";

  UUID getId();

  UUID getUserId();

  UUID getStarterId();

  UUID getReceiverId();

  Boolean getIsSystemResource();

  Long getCreationRecordId();

  Long getModificationRecordId();

  BattleStatus getStatus();

  BattleType getType();

  UUID getWinnerId();

  BattleEntityBuilder toBuilder();
}
