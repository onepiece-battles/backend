package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.I18nText;
import java.util.UUID;

@Entity
public interface CharacterInGroupLite {
  UUID getId();
  I18nText getName();

  String getPosition();

  Boolean getOnceBelongedTo();
}
