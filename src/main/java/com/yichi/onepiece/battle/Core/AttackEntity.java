package com.yichi.onepiece.battle.Core;

import com.yichi.infra.support.entity.Entity;

@Entity
public interface AttackEntity {
  Integer getAttack();

  AttackEntityBuilder toBuilder();
}
