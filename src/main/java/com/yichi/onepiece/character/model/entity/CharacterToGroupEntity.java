package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface CharacterToGroupEntity {
  UUID getId();

  UUID getCharacterId();

  UUID getGroupId();

  String getPosition();

  Boolean getOnceBelongedTo();
}
