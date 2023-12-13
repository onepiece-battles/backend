package com.yichi.onepiece.character.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.CharacterType;
import java.util.UUID;

@Entity
public interface CharacterTypeEntity {
  UUID getId();

  UUID getCharacterId();

  CharacterType getCharacterType();
}
