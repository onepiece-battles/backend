package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.model.Battle;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = EntityBuilder.class)
public interface BattleConverter {
  BattleConverter INSTANCE = Mappers.getMapper(BattleConverter.class);

  Battle toBattle(BattleEntity battleEntity);


  BattleEntity toBattleEntity(Battle battle);
}
