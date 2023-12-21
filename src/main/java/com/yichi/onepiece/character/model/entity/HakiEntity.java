package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.Haki;
import java.util.UUID;

@Entity
public interface HakiEntity {
  UUID getId();

  UUID getCharacterId();

  Haki getHaki();
}
