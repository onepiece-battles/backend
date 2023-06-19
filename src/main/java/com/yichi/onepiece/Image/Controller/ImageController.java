package com.yichi.onepiece.Image.Controller;

import com.yichi.onepiece.api.ImageApi;
import com.yichi.onepiece.model.ImageType;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ImageController implements ImageApi {
  @Override
  public ResponseEntity<Resource> addImage(UUID id, ImageType type, MultipartFile profile_image) {
    return null;
  }

  @Override
  public ResponseEntity<Resource> getImage(UUID id) {
    return null;
  }
}
