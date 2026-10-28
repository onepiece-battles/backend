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
import com.yichi.onepiece.image.dto.ImageAccess;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.Date;
import java.time.Instant;
import java.time.Duration;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

@Service
public class ImageService {
  private static final int MAX_ATTACHMENT_BASE64_LENGTH = 8 * 1024 * 1024;
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
    if (profileImage == null || profileImage.isBlank()) {
      throw new FileStorageException("Image content is empty");
    }

    int contentOffset = getBase64ContentOffset(profileImage);
    int encodedLength = profileImage.length() - contentOffset;
    if (encodedLength > MAX_ATTACHMENT_BASE64_LENGTH) {
      throw new FileStorageException("Image content exceeds the 6 MB upload limit");
    }
    if (encodedLength < 2 || encodedLength % 4 == 1) {
      throw new FileStorageException("Image content is not valid Base64");
    }

    try {
      InputStream encodedStream = new CharSequenceAsciiInputStream(profileImage, contentOffset);
      return AliCloudObjectStorage.putFileStream(
          Base64.getDecoder().wrap(encodedStream),
          decodedBase64Length(profileImage, contentOffset));
    } catch (IllegalArgumentException e) {
      throw new FileStorageException("Image content is not valid Base64", e);
    }
  }

  public String getImage(String uri, @Nullable Boolean fullImage) {
    try (InputStream imageStream = AliCloudObjectStorage.getImageStream(uri, fullImage)) {
        return EncryptHelper.base64Encode(imageStream.readAllBytes());
    } catch (Exception e) {
      if (fullImage == null || !fullImage) {
        logger.warn("failed to load compressed image {}, falling back to original image", uri, e);
        try (InputStream imageStream = AliCloudObjectStorage.getImageStream(uri, true)) {
          return EncryptHelper.base64Encode(imageStream.readAllBytes());
        } catch (Exception fallbackException) {
          logger.error("failed to load original image after compressed image fallback {}", uri, fallbackException);
        }
      } else {
        logger.error("failed to load original image {}", uri, e);
      }
    }
    throw new RuntimeException("file retrieval failed");
  }

  public ImageAccess getImageAccess(String uri, @Nullable Boolean fullImage)
      throws FileStorageException {
    Date expiration = Date.from(Instant.now().plus(Duration.ofMinutes(15)));
    String original = AliCloudObjectStorage.signImageUrl(uri, true, expiration).toExternalForm();
    String display = Boolean.TRUE.equals(fullImage) ? original
        : AliCloudObjectStorage.signImageUrl(uri, false, expiration).toExternalForm();
    return new ImageAccess(display, original, expiration.getTime());
  }

  private int getBase64ContentOffset(String image) {
    int dataSeparator = image.indexOf(',');
    if (image.startsWith("data:") && dataSeparator >= 0) {
      return dataSeparator + 1;
    }
    return 0;
  }

  private long decodedBase64Length(String image, int contentOffset) {
    int encodedLength = image.length() - contentOffset;
    int padding = 0;
    if (image.endsWith("==")) {
      padding = 2;
    } else if (image.endsWith("=")) {
      padding = 1;
    }
    return (encodedLength * 6L / 8L) - padding;
  }

  /**
   * Exposes an existing Base64 string as ASCII bytes without allocating another full-size byte array.
   */
  private static final class CharSequenceAsciiInputStream extends InputStream {
    private final CharSequence value;
    private int position;

    private CharSequenceAsciiInputStream(CharSequence value, int offset) {
      this.value = value;
      this.position = offset;
    }

    @Override
    public int read() throws IOException {
      if (position >= value.length()) {
        return -1;
      }
      char current = value.charAt(position++);
      if (current > 0x7F) {
        throw new IOException("Base64 content contains a non-ASCII character");
      }
      return current;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      if (position >= value.length()) {
        return -1;
      }
      int count = Math.min(length, value.length() - position);
      for (int i = 0; i < count; i++) {
        char current = value.charAt(position++);
        if (current > 0x7F) {
          throw new IOException("Base64 content contains a non-ASCII character");
        }
        buffer[offset + i] = (byte) current;
      }
      return count;
    }
  }
}
