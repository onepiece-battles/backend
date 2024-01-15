package com.yichi.onepiece.image.service;

import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.user.service.UserService;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ImageService {
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
      case GROUP -> {
        return groupService.updateGroupImage(targetId, profileImage);
      }
    }
    throw new RuntimeException("file upload with id failed");
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
