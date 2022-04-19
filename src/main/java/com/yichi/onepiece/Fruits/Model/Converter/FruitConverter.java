package com.yichi.onepiece.Fruits.Model.Converter;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.Fruits.Model.FruitEntity;
import com.yichi.onepiece.model.AddFruitRequest;
import com.yichi.onepiece.model.Fruit;
import com.yichi.onepiece.model.UpdateFruitRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = EntityBuilder.class)
public interface FruitConverter {
  FruitConverter INSTANCE = Mappers.getMapper(FruitConverter.class);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "ownerId", ignore = true)
  FruitEntity toFruitEntity(AddFruitRequest request);

  FruitEntity toFruitEntity(UpdateFruitRequest request);

  Fruit toFruit(FruitEntity entity);
}
