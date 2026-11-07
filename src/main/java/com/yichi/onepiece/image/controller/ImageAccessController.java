package com.yichi.onepiece.image.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.image.dto.ImageAccess;
import com.yichi.onepiece.image.service.ImageService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** JWT authentication is enforced by the same /api security chain as the other image endpoints. */
@RestController
public class ImageAccessController {
  private final ImageService imageService;

  public ImageAccessController(ImageService imageService) {
    this.imageService = imageService;
  }

  @PostMapping("/api/onepiece/image/get_image_url")
  public ResponseEntity<ImageAccess> getImageUrl(@RequestBody ImageUrlRequest request) {
    try {
      var access = imageService.getImageAccess(request.uri(), request.fullImage());
      return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(access);
    } catch (FileStorageException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OSS image URI");
    }
  }

  /** Local request DTO so this endpoint no longer depends on the removed base64 getImage model. */
  public record ImageUrlRequest(String uri, @JsonProperty("full_image") Boolean fullImage) {}
}
