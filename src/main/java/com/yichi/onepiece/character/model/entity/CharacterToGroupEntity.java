package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.GroupPosition;

import java.util.UUID;

@Entity
public interface CharacterToGroupEntity {
  UUID getId();

  UUID getCharacterId();

  UUID getGroupId();

  GroupPosition getPosition();

  Boolean getOnceBelongedTo();

  CharacterToGroupEntityBuilder toBuilder();
}
