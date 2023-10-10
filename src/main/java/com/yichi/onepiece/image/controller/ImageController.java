package com.yichi.onepiece.image.controller;

import com.yichi.onepiece.api.ImageApi;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.model.GetImageRequest;
import com.yichi.onepiece.model.ResourceType;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ImageController implements ImageApi {
  private final ImageService imageService;

  public ImageController(ImageService imageService) {
    this.imageService = imageService;
  }

  @Override
  public ResponseEntity<String> addImage(UUID id, ResourceType resourceType, String image) {
    var encodedImage = imageService.uploadImage(id, resourceType, image);
    return ResponseEntity.ok(encodedImage);
  }

  @Override
  public ResponseEntity<Resource> getImage(GetImageRequest request) {
    var image = imageService.getImage(request.getId(), request.getType());
    return ResponseEntity.ok(new ByteArrayResource(image));
  }
}
