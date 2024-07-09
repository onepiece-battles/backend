package com.yichi.onepiece.fruit.model.converter;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.fruit.model.FruitEntity;
import com.yichi.onepiece.fruit.model.FruitListView;
import com.yichi.onepiece.model.CreateFruitRequest;
import com.yichi.onepiece.model.Fruit;
import com.yichi.onepiece.model.FruitFullView;
import com.yichi.onepiece.model.UpdateFruitRequest;
import com.yichi.onepiece.utils.EncryptHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = EntityBuilder.class)
public interface FruitConverter {
  FruitConverter INSTANCE = Mappers.getMapper(FruitConverter.class);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "profileImage", ignore = true)
  @Mapping(target = "imageUri", ignore = true)
  @Mapping(target = "creationRecordId", ignore = true)
  @Mapping(target = "modificationRecordId", ignore = true)
  @Mapping(target = "systemResource", ignore = true)
  FruitEntity toFruitEntity(CreateFruitRequest request);

  @Mapping(target = "profileImage", ignore = true)
  @Mapping(target = "creationRecordId", ignore = true)
  @Mapping(target = "modificationRecordId", ignore = true)
  @Mapping(target = "systemResource", ignore = true)
  @Mapping(target = "imageUri", ignore = true)
  FruitEntity toFruitEntity(UpdateFruitRequest request);

  @Mapping(target = "fruitHolders", ignore = true)
  Fruit toFruit(FruitEntity entity);

  @Mapping(target = "fruitHolders", ignore = true)
  FruitFullView toFruitFullView(com.yichi.onepiece.fruit.model.FruitFullView fruitFullView);

  @Mapping(target = "fruitHolders", ignore = true)
  com.yichi.onepiece.model.FruitListView toFruitListView(FruitListView fruitListView);

  default String blobToString(Byte[] profileByteArray) {
    if (profileByteArray == null) {
      return null;
    }
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(profileByteArray));
  }

  default Byte[] stringToBlob(String string) {
    if (string == null) {
      return null;
    }
    return ArrayUtils.toObject(EncryptHelper.base64Decode(string));
  }
}
