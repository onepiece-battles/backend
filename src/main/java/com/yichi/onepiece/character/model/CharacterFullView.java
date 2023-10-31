package com.yichi.onepiece.character.model;

import com.yichi.infra.support.entity.Entity;
import java.util.List;
import java.util.UUID;

@Entity
public interface CharacterFullView extends CharacterEntity {
  List<UUID> getBattleIds();
}
