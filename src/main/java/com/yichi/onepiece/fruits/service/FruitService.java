package com.yichi.onepiece.fruits.service;

import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.fruits.model.FruitDao;
import com.yichi.onepiece.fruits.model.FruitEntity;
import com.yichi.onepiece.fruits.model.converter.FruitConverter;
import com.yichi.onepiece.model.*;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

  public ListFruitResponse listFruit(ListFruitRequest request) {
    var totalSize = fruitDao.countFruit();
    // pageIndex is 1- based. offset is 0- based.
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return new ListFruitResponse().fruits(List.of()).totalSize(totalSize);
    }
    var fruits =
        fruitDao.listFruits(offset, request.getPageSize()).stream()
            .map(FruitConverter.INSTANCE::toFruit)
            .toList();
    return new ListFruitResponse().totalSize(totalSize).fruits(fruits);
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

  public List<FruitEntity> getFruitsByIds(List<UUID> fruitIds) {
    if (fruitIds.isEmpty()) {
      return List.of();
    }
    return fruitDao.batchFindById(fruitIds);
  }

  public byte[] updateProfile(UUID fruitId, MultipartFile profile) {
    Byte[] profileByte = new Byte[0];
    try {
      profileByte = ArrayUtils.toObject(profile.getInputStream().readAllBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
    var fruit =
        fruitDao
            .findById(fruitId)
            .orElseThrow(ExceptionUtils.notFound("fruit with id %s not found", fruitId));
    var updatedFruit = fruit.toBuilder().setProfileImage(profileByte).build();
    fruitDao.update(updatedFruit);
    return ArrayUtils.toPrimitive(updatedFruit.getProfileImage());
  }

  public byte[] getProfile(UUID fruitId) {
    var fruit =
        fruitDao
            .findById(fruitId)
            .orElseThrow(ExceptionUtils.notFound("fruit with id %s not found", fruitId));
    return ArrayUtils.toPrimitive(fruit.getProfileImage());
  }
}
