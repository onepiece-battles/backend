package com.yichi.onepiece.image.service;

import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.user.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;

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

  public String uploadImage(UUID targetId, ResourceType resourceType, String profileImage) {
    switch (resourceType) {
      case USER -> {
        return userService.updateProfile(targetId, profileImage);
      }
      case FRUIT -> {
        return fruitService.updateProfile(targetId, profileImage);
      }
      case CHARACTER -> {
        return characterService.updateProfile(targetId, profileImage);
      }
    }
    throw new RuntimeException("file upload with id failed");
  }

  public byte[] getImage(UUID resourceId, ResourceType resourceType) {
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
