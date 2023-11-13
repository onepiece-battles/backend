package com.yichi.onepiece.fruit.service;

import com.yichi.onepiece.character.helper.CharacterHelper;
import com.yichi.onepiece.character.model.CharacterEntity;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.fruit.model.FruitDao;
import com.yichi.onepiece.fruit.model.FruitEntity;
import com.yichi.onepiece.fruit.model.converter.FruitConverter;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.utils.EncryptHelper;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;

@Service
public class FruitService {
  private static final ResourceType RESOURCE_TYPE = ResourceType.FRUIT;
  private final FruitDao fruitDao;
  private final ActionRecordService actionRecordService;
  private final CharacterHelper characterHelper;
  private final UserService userService;

  public FruitService(
      FruitDao fruitDao,
      ActionRecordService actionRecordService,
      CharacterHelper characterHelper,
      UserService userService) {
    this.fruitDao = fruitDao;
    this.actionRecordService = actionRecordService;
    this.characterHelper = characterHelper;
    this.userService = userService;
  }

  public Fruit addFruit(AddFruitRequest request) {
    var actionRecord = actionRecordService.create();
    var isSystemResource = UserHelper.hasAdminPermission();
    var fruitEntity =
        FruitConverter.INSTANCE.toFruitEntity(request).toBuilder()
            .setCreationRecordId(actionRecord.getId())
            .setModificationRecordId(actionRecord.getId())
            .setIsSystemResource(isSystemResource)
            .build();
    fruitDao.insert(fruitEntity);
    var characterNames =
        characterHelper.listByFruitId(fruitEntity.getId()).stream()
            .map(CharacterEntity::getName)
            .toList();
    return FruitConverter.INSTANCE.toFruit(fruitEntity).holders(characterNames);
  }

  public ListFruitResponse listFruit(ListFruitRequest request) {
    var totalSize = fruitDao.countFruit();
    // pageIndex is 1- based. offset is 0- based.
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return new ListFruitResponse().fruits(List.of()).totalSize(totalSize);
    }
    var userId = UserHelper.getCurrentUserId();
    var resourceList = userService.getResourceIdsByUserIdResourceType(userId, RESOURCE_TYPE);
    var fruits =
        fruitDao
            .listFruits(
                request.getFruitFilter(),
                request.getSortKeys(),
                userId,
                resourceList,
                offset,
                request.getPageSize())
            .stream()
            .map(FruitConverter.INSTANCE::toFruit)
            .map(
                fruit -> {
                  var characterNames =
                      characterHelper.listByFruitId(fruit.getId()).stream()
                          .map(CharacterEntity::getName)
                          .toList();
                  return fruit.holders(characterNames);
                })
            .toList();
    return new ListFruitResponse().totalSize(totalSize).fruits(fruits);
  }

  public Fruit updateFruit(UpdateFruitRequest request) {
    var modificationRecord = actionRecordService.create();
    fruitDao
        .findById(request.getId())
        .orElseThrow(
            ExceptionUtils.notFound(
                "Can not update fruit with id: %s, not found", request.getId()));
    var fruitEntity =
        FruitConverter.INSTANCE.toFruitEntity(request).toBuilder()
            .setModificationRecordId(modificationRecord.getId())
            .build();
    fruitDao.update(fruitEntity);
    var updatedFruit = fruitDao.findById(request.getId()).orElseThrow();
    var characterNames =
        characterHelper.listByFruitId(fruitEntity.getId()).stream()
            .map(CharacterEntity::getName)
            .toList();
    return FruitConverter.INSTANCE.toFruit(updatedFruit).holders(characterNames);
  }

  public List<FruitEntity> getFruitsByIds(List<UUID> fruitIds) {
    if (fruitIds.isEmpty()) {
      return List.of();
    }
    return fruitDao.batchFindById(fruitIds);
  }

  public List<FruitEntity> listByCharacterId(UUID characterId) {
    return fruitDao.listByCharacterId(characterId);
  }

  public String updateProfile(UUID fruitId, String profile) {
    Byte[] profileByte = ArrayUtils.toObject(EncryptHelper.base64Decode(profile));
    var modificationRecord = actionRecordService.create();
    var fruit =
        fruitDao
            .findById(fruitId)
            .orElseThrow(ExceptionUtils.notFound("fruit with id %s not found", fruitId));
    var updatedFruit =
        fruit.toBuilder()
            .setModificationRecordId(modificationRecord.getId())
            .setProfileImage(profileByte)
            .build();
    fruitDao.update(updatedFruit);
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(updatedFruit.getProfileImage()));
  }

  public String getProfile(UUID fruitId) {
    var fruit =
        fruitDao
            .findById(fruitId)
            .orElseThrow(ExceptionUtils.notFound("fruit with id %s not found", fruitId));
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(fruit.getProfileImage()));
  }
}
