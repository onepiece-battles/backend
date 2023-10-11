package com.yichi.onepiece.image.controller;

import com.yichi.onepiece.api.ImageApi;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.model.GetImageRequest;
import com.yichi.onepiece.model.GetImageResponse;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.UploadImageRequest;
import com.yichi.onepiece.model.UploadImageResponse;
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
  public ResponseEntity<UploadImageResponse> uploadImage(UploadImageRequest request) {
    var encodedImage = imageService.uploadImage(request.getResourceId(), request.getType(), request.getImage());
    return ResponseEntity.ok(new UploadImageResponse().image(encodedImage));
  }

  @Override
  public ResponseEntity<GetImageResponse> getImage(GetImageRequest request) {
    var image = imageService.getImage(request.getResourceId(), request.getResourceType());
    return ResponseEntity.ok(new GetImageResponse().image(image));
  }
}
