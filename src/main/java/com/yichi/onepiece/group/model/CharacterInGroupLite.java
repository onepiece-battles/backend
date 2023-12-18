package com.yichi.onepiece.group.model;

import com.yichi.infra.support.entity.Entity;

@Entity
public interface CharacterInGroupLite {
  String getCharacterName();
  String getCharacterPosition();
  String getOnceBelongedTo();
}
