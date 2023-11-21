package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.character.model.converter.CharacterEntityConverter;
import com.yichi.onepiece.model.Battle;
import com.yichi.onepiece.model.BattleFullView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EntityBuilder.class, CharacterEntityConverter.class})
public interface BattleConverter {
  BattleConverter INSTANCE = Mappers.getMapper(BattleConverter.class);

  @Mapping(target = "starterName", ignore = true)
  @Mapping(target = "receiverName", ignore = true)
  @Mapping(target = "userName", ignore = true)
  @Mapping(target = "creationTimestamp", ignore = true)
  @Mapping(target = "modificationTimestamp", ignore = true)
  Battle toBattle(BattleEntity battleEntity);

  @Mapping(target = "starterName", source = "starter.name")
  @Mapping(target = "starterId", source = "starter.id")
  @Mapping(target = "receiverName", source = "receiver.name")
  @Mapping(target = "receiverId", source = "receiver.id")
  @Mapping(target = "userName", source = "user.name")
  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "creationRecordId", source = "creationRecord.id")
  @Mapping(target = "creationTimestamp", source = "creationRecord.actionTimestamp")
  @Mapping(target = "modificationRecordId", source = "modificationRecord.id")
  @Mapping(target = "modificationTimestamp", source = "modificationRecord.actionTimestamp")
  Battle toBattle(BattleFullViewEntity battleFullViewEntity);

  BattleFullView toBattleFullView(BattleFullViewEntity battleFullViewEntity);
}
