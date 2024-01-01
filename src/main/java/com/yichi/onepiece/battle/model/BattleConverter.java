package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.character.model.converter.CharacterEntityConverter;
import com.yichi.onepiece.model.Battle;
import com.yichi.onepiece.model.BattleLite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EntityBuilder.class, CharacterEntityConverter.class})
public interface BattleConverter {
  BattleConverter INSTANCE = Mappers.getMapper(BattleConverter.class);

  com.yichi.onepiece.model.BattleListView toBattleListView(BattleListView battleEntity);

  Battle toBattle(BattleEntity battleEntity);

  com.yichi.onepiece.model.BattleFullView toBattleFullView(BattleFullView battleFullViewEntity);

  @Mapping(target = "battleReportStage", ignore = true)
  @Mapping(target = "battleStatus", source = "status")
  BattleLite toBattleLite(BattleEntity battleEntity);
}
