package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.I18nText;

@Entity
public interface CharacterInGroupLite {
  I18nText getName();

  String getPosition();

  Boolean getOnceBelongedTo();
}
