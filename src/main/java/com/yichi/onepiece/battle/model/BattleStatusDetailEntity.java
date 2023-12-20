package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.I18nText;
import java.util.UUID;

@Entity
public interface BattleStatusDetailEntity {
  UUID getId();

  UUID getBattleId();

  Integer getBattleStatusRoundNumber();

  I18nText getAttackSide();

  CurrentCharacterEntity getAttacker();

  CurrentCharacterEntity getDefender();

  BattleStatusDetailEntityBuilder toBuilder();
}
