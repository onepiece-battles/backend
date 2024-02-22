package com.yichi.onepiece.fruit.service;

import com.yichi.onepiece.character.helper.CharacterHelper;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.fruit.model.FruitDao;
import com.yichi.onepiece.fruit.model.FruitEntity;
import com.yichi.onepiece.fruit.model.FruitListView;
import com.yichi.onepiece.fruit.model.converter.FruitConverter;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FruitService {
  private final Logger logger = LoggerFactory.getLogger(FruitService.class);
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

  public Fruit addFruit(CreateFruitRequest request) {
    var actionRecord = actionRecordService.create();
    var fruitEntity =
        FruitConverter.INSTANCE.toFruitEntity(request).toBuilder()
            .setCreationRecordId(actionRecord.getId())
            .setModificationRecordId(actionRecord.getId())
            .setUserId(UserHelper.getCurrentUserId())
            .setSystemResource(UserHelper.hasNikaPermission())
            .build();
    fruitDao.insert(fruitEntity);
    var characterNames =
        characterHelper.listByFruitId(fruitEntity.getId()).stream()
            .map(CharacterEntity::getName)
            .toList();
    return FruitConverter.INSTANCE.toFruit(fruitEntity).fruitHolders(characterNames);
  }

  public FruitFullView getFruit(GetFruitRequest request) {
    var fruitFullView  = fruitDao.getFruitFullViewById(request.getFruitId()).orElseThrow();
    return FruitConverter.INSTANCE.toFruitFullView(fruitFullView);
  }

  public ListFruitResponse listFruit(ListFruitRequest request) {
    var userId = UserHelper.getCurrentUserId();
    var resourceList = userService.getResourceIdsByUserIdResourceType(userId, RESOURCE_TYPE);
    // need to take external resource into consideration
    var totalSize = fruitDao.searchCount(userId, request.getFruitFilter(), resourceList);
    // pageIndex is 1- based. offset is 0- based.
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return new ListFruitResponse().fruits(List.of()).totalSize(totalSize);
    }
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
            .map(FruitConverter.INSTANCE::toFruitListView)
            .toList();
    return new ListFruitResponse().totalSize(totalSize).fruits(fruits);
  }

  public UpdateFruitResponse updateFruit(UpdateFruitRequest request) {
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
    var response = new UpdateFruitResponse();
    return response.result(
            UserFacingConstants.getConstant(UserFacingConstantsKey.UPDATE_SUCCESS)
                    .getDisplayName()).success(Boolean.TRUE);
  }

  public List<FruitEntity> getFruitsByIds(List<UUID> fruitIds) {
    if (fruitIds.isEmpty()) {
      return List.of();
    }
    return fruitDao.batchFindById(fruitIds);
  }

  public List<FruitListView> listByCharacterId(UUID characterId) {
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
