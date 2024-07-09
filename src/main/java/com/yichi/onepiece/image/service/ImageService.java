package com.yichi.onepiece.image.service;

import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.user.service.UserService;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ImageService {
  Logger logger = LoggerFactory.getLogger(ImageService.class);
  private final CharacterService characterService;
  private final FruitService fruitService;
  private final UserService userService;
  private final GroupService groupService;

  public ImageService(
      CharacterService characterService,
      FruitService fruitService,
      UserService userService,
      GroupService groupService) {
    this.characterService = characterService;
    this.fruitService = fruitService;
    this.userService = userService;
    this.groupService = groupService;
  }

  public I18nText uploadImage(UUID targetId, ResourceType resourceType, String profileImage)  {
    try {
      String uri = uploadImage(profileImage);
      switch (resourceType) {
        case USER -> {
           userService.updateProfile(targetId, uri);
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
      return UserFacingConstants.getConstant(UserFacingConstantsKey.IMAGE_UPLOAD_SUCCESS)
              .getDisplayName();
    } catch (Exception e) {
      logger.error(e.toString());
    }
    return UserFacingConstants.getConstant(UserFacingConstantsKey.IMAGE_UPLOAD_FAILED)
            .getDisplayName();
  }

  // encode image with base64 and upload this image to alicloud.
  public String uploadImage(String profileImage) throws FileStorageException {
    return AliCloudObjectStorage.putFileStream(new ByteArrayInputStream(EncryptHelper.base64Decode(profileImage)));
  }

  public String getImage(UUID resourceId, ResourceType resourceType) {
    switch (resourceType) {
      case USER -> {
        return userService.getProfile(resourceId);
      }
      case FRUIT -> {
        return fruitService.getProfile(resourceId);
      }
      case CHARACTER -> {
        return characterService.getProfile(resourceId);
      }
    }
    throw new RuntimeException("file retrieval failed");
  }
}
