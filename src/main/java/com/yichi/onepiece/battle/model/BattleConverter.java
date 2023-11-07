package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.character.model.converter.CharacterEntityConverter;
import com.yichi.onepiece.model.Battle;
import com.yichi.onepiece.model.BattleFullView;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EntityBuilder.class, CharacterEntityConverter.class})
public interface BattleConverter {
  BattleConverter INSTANCE = Mappers.getMapper(BattleConverter.class);

  Battle toBattle(BattleEntity battleEntity);


  BattleEntity toBattleEntity(Battle battle);

  BattleFullView toBattleFullView(BattleFullViewEntity battleFullViewEntity);
}
