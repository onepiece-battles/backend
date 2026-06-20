package com.yichi.onepiece.image.service;

import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.file.model.FileCategory;
import com.yichi.onepiece.file.model.FileObjectEntityBuilder;
import com.yichi.onepiece.file.service.FileObjectService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;
import java.util.UUID;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

@Service
public class ImageService {
  Logger logger = LoggerFactory.getLogger(ImageService.class);
  private final CharacterService characterService;
  private final FruitService fruitService;
  private final UserService userService;
  private final GroupService groupService;
  private final FileObjectService fileObjectService;

  private final ActionRecordService actionRecordService;

  private static final Map<ResourceType, FileCategory> map = Map.of(
          ResourceType.CHARACTER, FileCategory.CHARACTER_IMAGE,
          ResourceType.USER, FileCategory.USER_IMAGE,
          ResourceType.FRUIT, FileCategory.FRUIT_IMAGE,
          ResourceType.GROUP, FileCategory.CHARACTER_IMAGE
  );

  public ImageService(
          CharacterService characterService,
          FruitService fruitService,
          UserService userService,
          GroupService groupService, FileObjectService fileObjectService, ActionRecordService actionRecordService) {
    this.characterService = characterService;
    this.fruitService = fruitService;
    this.userService = userService;
    this.groupService = groupService;
    this.fileObjectService = fileObjectService;
    this.actionRecordService = actionRecordService;
  }

  public I18nText uploadImage(UUID targetId, ResourceType resourceType, String profileImage)  {
    if (ResourceType.USER.equals(resourceType)
            && !UserHelper.hasAdminPermission()
            && !UserHelper.getCurrentUserId().equals(targetId)) {
      throw new PermissionDeniedException("Access Denied");
    }
    try {
      String uri = uploadImage(profileImage);
      switch (resourceType) {
        case USER -> {
           userService.updateProfile(targetId, uri, profileImage);
        }
        case FRUIT -> {
           fruitService.updateProfile(targetId, uri);
        }
        case CHARACTER -> {
           characterService.updateProfile(targetId, uri);
        }
        case GROUP -> {
           groupService.updateGroupImage(targetId, uri);
        }
      }
      var creationRecord = actionRecordService.create();
      File imageFile = new File(uri);
      var fileObjectEntity =
              FileObjectEntityBuilder.newBuilder()
                      .setFileName(imageFile.getName())
                      .setFileCategory(map.get(resourceType))
                      .setHttpMimeType("application/image")
                      .setCreatorId(UserHelper.getCurrentUserId())
                      .setCreationRecordId(creationRecord.getId())
                      .setOssUri(uri)
                      .setResourceId(targetId)
                      .build();
      fileObjectService.add(fileObjectEntity);
      return UserFacingConstants.getConstant(UserFacingConstantsKey.IMAGE_UPLOAD_SUCCESS)
              .getDisplayName();
    } catch (Exception e) {
      logger.error(e.toString());
    }
    return UserFacingConstants.getConstant(UserFacingConstantsKey.IMAGE_UPLOAD_FAILED)
            .getDisplayName();
  }

  // encode image with base64 and upload this image to alicloud. the return value is the URI of this image
  public String uploadImage(String profileImage) throws FileStorageException {
    return AliCloudObjectStorage.putFileStream(new ByteArrayInputStream(EncryptHelper.base64Decode(profileImage)));
  }

  public String getImage(String uri, @Nullable Boolean fullImage) {
    try {
        return EncryptHelper.base64Encode(AliCloudObjectStorage.getImageStream(uri,fullImage).readAllBytes());
    } catch (Exception e) {
      logger.error(e.toString());
    }
    throw new RuntimeException("file retrieval failed");
  }
}
