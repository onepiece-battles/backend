package com.yichi.onepiece.character.model.converter;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.character.model.entity.CharacterFullView;
import com.yichi.onepiece.character.model.entity.CharacterListView;
import com.yichi.onepiece.fruit.model.converter.FruitConverter;
import com.yichi.onepiece.model.Character;
import com.yichi.onepiece.model.CreateCharacterRequest;
import com.yichi.onepiece.model.GroupForCharacterView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EntityBuilder.class, FruitConverter.class})
public interface CharacterEntityConverter {
  CharacterEntityConverter INSTANCE = Mappers.getMapper(CharacterEntityConverter.class);

  Character toCharacter(CharacterEntity entity);

  com.yichi.onepiece.model.CharacterListView toCharacterListView(
      CharacterListView characterListView);

  com.yichi.onepiece.model.CharacterFullView toCharacterFullView(
      CharacterFullView characterFullView);

  GroupForCharacterView toGroupForCharacterView(
      com.yichi.onepiece.character.model.entity.GroupForCharacterView groupForCharacterView);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "profileImage", ignore = true)
  @Mapping(target = "systemResource", ignore = true)
  @Mapping(target = "creationRecordId", ignore = true)
  @Mapping(target = "modificationRecordId", ignore = true)
  @Mapping(target = "score", ignore = true)
  @Mapping(target = "baseAttack", source = "attack")
  @Mapping(target = "baseDefense", source = "defense")
  @Mapping(target = "baseIntelligence", source = "intelligence")
  @Mapping(target = "baseSpeed", source = "speed")
  @Mapping(target = "baseBlood", source = "blood")
  @Mapping(target = "baseGroupGain", source = "groupGain")
  CharacterEntity toCharacterEntity(CreateCharacterRequest addCharacterRequest);

  @Mapping(target = "creationRecordId", ignore = true)
  @Mapping(target = "modificationRecordId", ignore = true)
  @Mapping(target = "baseAttack", source = "attack")
  @Mapping(target = "baseDefense", source = "defense")
  @Mapping(target = "baseIntelligence", source = "intelligence")
  @Mapping(target = "baseSpeed", source = "speed")
  @Mapping(target = "baseBlood", source = "blood")
  @Mapping(target = "baseGroupGain", source = "groupGain")
  @Mapping(target = "attack", ignore = true)
  @Mapping(target = "defense", ignore = true)
  @Mapping(target = "intelligence", ignore = true)
  @Mapping(target = "speed", ignore = true)
  @Mapping(target = "blood", ignore = true)
  @Mapping(target = "groupGain", ignore = true)
  CharacterEntity toCharacterEntity(Character character);
}
