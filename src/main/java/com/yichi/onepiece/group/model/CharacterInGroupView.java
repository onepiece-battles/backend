package com.yichi.onepiece.group.model;

import com.yichi.infra.support.entity.Entity;

@Entity
public interface CharacterInGroupView extends GroupEntity {
  Boolean getOnceBelongedTo();
  String getPosition();
}
