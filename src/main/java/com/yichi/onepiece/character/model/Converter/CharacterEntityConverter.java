package com.yichi.onepiece.character.model.Converter;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.character.model.CharacterEntity;
import com.yichi.onepiece.model.AddCharacterRequest;
import com.yichi.onepiece.model.Character;
import org.apache.commons.lang3.ArrayUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@Mapper(uses = EntityBuilder.class)
public interface CharacterEntityConverter {
  CharacterEntityConverter INSTANCE = Mappers.getMapper(CharacterEntityConverter.class);

  Character toCharacter(CharacterEntity entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "profileImage", ignore = true)
  @Mapping(target = "fruits", ignore = true)
  @Mapping(target = "hasFruits", ignore = true)
  CharacterEntity toCharacterEntity(AddCharacterRequest addCharacterRequest);

  default Resource blobToResource(Byte[] profileByteArray) {
    if (profileByteArray == null) {
      return null;
    }
    return new ByteArrayResource(ArrayUtils.toPrimitive(profileByteArray));
  }
}
