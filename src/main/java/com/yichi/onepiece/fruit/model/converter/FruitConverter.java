package com.yichi.onepiece.fruit.model.converter;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.fruit.model.FruitEntity;
import com.yichi.onepiece.model.AddFruitRequest;
import com.yichi.onepiece.model.Fruit;
import com.yichi.onepiece.model.UpdateFruitRequest;
import org.apache.commons.lang3.ArrayUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

@Mapper(uses = EntityBuilder.class)
public interface FruitConverter {
  FruitConverter INSTANCE = Mappers.getMapper(FruitConverter.class);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "ownerId", ignore = true)
  @Mapping(target = "profileImage", ignore = true)
  @Mapping(target = "creationRecordId", ignore = true)
  @Mapping(target = "modificationRecordId", ignore = true)
  @Mapping(target = "isSystemResource", ignore = true)
  FruitEntity toFruitEntity(AddFruitRequest request);

  @Mapping(target = "profileImage", ignore = true)
  @Mapping(target = "creationRecordId", ignore = true)
  @Mapping(target = "modificationRecordId", ignore = true)
  @Mapping(target = "isSystemResource", ignore = true)
  FruitEntity toFruitEntity(UpdateFruitRequest request);

  Fruit toFruit(FruitEntity entity);

  default Resource blobToResource(Byte[] profileByteArray) {
    if (profileByteArray == null) {
      return null;
    }
    return new ByteArrayResource(ArrayUtils.toPrimitive(profileByteArray));
  }
}
