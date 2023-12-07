package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.character.model.converter.CharacterEntityConverter;
import com.yichi.onepiece.model.Battle;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EntityBuilder.class, CharacterEntityConverter.class})
public interface BattleConverter {
  BattleConverter INSTANCE = Mappers.getMapper(BattleConverter.class);


  com.yichi.onepiece.model.BattleListView toBattleListView(BattleListView battleEntity);
  Battle toBattle(BattleEntity battleEntity);

  com.yichi.onepiece.model.BattleFullView toBattleFullView(BattleFullView battleFullViewEntity);
}
