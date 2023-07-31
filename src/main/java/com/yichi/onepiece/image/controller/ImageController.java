package com.yichi.onepiece.image.controller;

import com.yichi.onepiece.api.ImageApi;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.model.GetImageRequest;
import com.yichi.onepiece.model.ImageMetaInfo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ImageController implements ImageApi {
  private final ImageService imageService;

  public ImageController(ImageService imageService) {
    this.imageService = imageService;
  }

  @Override
  public ResponseEntity<Resource> addImage(ImageMetaInfo meta, MultipartFile image) {
    var resultImage = imageService.uploadImage(meta.getId(), meta.getType(), image);
    return ResponseEntity.ok(new ByteArrayResource(resultImage));
  }

  @Override
  public ResponseEntity<Resource> getImage(GetImageRequest request) {
    var image = imageService.getImage(request.getId(), request.getType());
    return ResponseEntity.ok(new ByteArrayResource(image));
  }
}
