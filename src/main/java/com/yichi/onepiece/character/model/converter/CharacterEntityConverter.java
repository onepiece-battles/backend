package com.yichi.onepiece.character.model.converter;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.character.model.entity.CharacterFullView;
import com.yichi.onepiece.character.model.entity.CharacterListView;
import com.yichi.onepiece.fruit.model.converter.FruitConverter;
import com.yichi.onepiece.model.Character;
import com.yichi.onepiece.model.CreateCharacterRequest;
import com.yichi.onepiece.model.GroupForCharacterView;
import com.yichi.onepiece.utils.EncryptHelper;
import org.apache.commons.lang3.ArrayUtils;
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
  @Mapping(target = "creationRecordId", ignore = true)
  @Mapping(target = "modificationRecordId", ignore = true)
  @Mapping(target = "systemResource", ignore = true)
  CharacterEntity toCharacterEntity(CreateCharacterRequest addCharacterRequest);

  default String blobToString(Byte[] profileByteArray) {
    if (profileByteArray == null) {
      return null;
    }
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(profileByteArray));
  }
}
