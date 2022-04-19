package com.yichi.onepiece.Fruits.Service;

import com.yichi.onepiece.Exception.ExceptionUtils;
import com.yichi.onepiece.Fruits.Model.Converter.FruitConverter;
import com.yichi.onepiece.Fruits.Model.FruitDao;
import com.yichi.onepiece.model.AddFruitRequest;
import com.yichi.onepiece.model.Fruit;
import com.yichi.onepiece.model.ListFruitRequest;
import com.yichi.onepiece.model.UpdateFruitRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FruitService {
  private final FruitDao fruitDao;

  public FruitService(FruitDao fruitDao) {
    this.fruitDao = fruitDao;
  }

  public Fruit addFruit(AddFruitRequest request) {
    var fruitEntity = FruitConverter.INSTANCE.toFruitEntity(request);
    fruitDao.insert(fruitEntity);
    return FruitConverter.INSTANCE.toFruit(fruitEntity);
  }

  public List<Fruit> listFruit(ListFruitRequest request) {
    var totalSize = fruitDao.countFruit();
    // pageIndex is 1- based. offset is 0- based.
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return List.of();
    }
    return fruitDao.listFruits(offset, request.getPageSize()).stream()
        .map(FruitConverter.INSTANCE::toFruit)
        .collect(Collectors.toList());
  }

  public Fruit updateFruit(UpdateFruitRequest request) {
    fruitDao
        .findById(request.getId())
        .orElseThrow(
            ExceptionUtils.notFound(
                "Can not update fruit with id: %s, not found", request.getId()));
    var fruitEntity = FruitConverter.INSTANCE.toFruitEntity(request);
    fruitDao.update(fruitEntity);
    var updatedFruit = fruitDao.findById(request.getId()).get();
    return FruitConverter.INSTANCE.toFruit(updatedFruit);
  }

  public void updateOwner(UUID fruitId, UUID ownId) {
    var fruitEntity =
        fruitDao
            .findById(fruitId)
            .orElseThrow(
                ExceptionUtils.notFound("Can not update fruit with id: %s, not found", fruitId));
    var updatedEntity = fruitEntity.toBuilder().setOwnerId(ownId).build();
    fruitDao.update(updatedEntity);
  }
}
