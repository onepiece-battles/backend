package com.yichi.onepiece.image.service;

import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.model.ImageType;
import com.yichi.onepiece.user.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {
  private final CharacterService characterService;
  private final FruitService fruitService;
  private final UserService userService;

  public ImageService(
      CharacterService characterService, FruitService fruitService, UserService userService) {
    this.characterService = characterService;
    this.fruitService = fruitService;
    this.userService = userService;
  }

  public byte[] uploadImage(UUID targetId, ImageType targetType, MultipartFile profileImage) {
    switch (targetType) {
      case USER_IMAGE -> {
        return userService.updateProfile(targetId, profileImage);
      }
      case FRUIT_IMAGE -> {
        return fruitService.updateProfile(targetId, profileImage);
      }
      case CHARACTER_IMAGE -> {
        return characterService.updateProfile(targetId, profileImage);
      }
    }
    throw new RuntimeException("file upload with id failed");
  }

  public byte[] getImage(UUID resourceId, ImageType targetType) {
    switch (targetType) {
      case USER_IMAGE -> {
        return userService.getProfile(resourceId);
      }
      case FRUIT_IMAGE -> {
        return fruitService.getProfile(resourceId);
      }
      case CHARACTER_IMAGE -> {
        return characterService.getProfile(resourceId);
      }
    }
    throw new RuntimeException("file retrieval failed");
  }
}
