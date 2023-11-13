package com.yichi.onepiece.character.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface CharacterToFruitEntity {
  UUID getId();
  UUID getCharacterId();
  UUID getFruitId();
  Boolean getWakeUp();
}
