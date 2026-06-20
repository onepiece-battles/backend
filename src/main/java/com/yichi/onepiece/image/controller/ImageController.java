package com.yichi.onepiece.image.controller;

import com.yichi.onepiece.api.ImageApi;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.image.service.ImageService;
import com.yichi.onepiece.model.GetImageRequest;
import com.yichi.onepiece.model.GetImageResponse;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.UploadImageRequest;
import com.yichi.onepiece.model.UploadImageResponse;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class ImageController implements ImageApi {
  private final ImageService imageService;
  private final IamService iamService;

  public ImageController(ImageService imageService, IamService iamService) {
    this.imageService = imageService;
    this.iamService = iamService;
  }
    @Override
    public ResponseEntity<GetImageResponse> getImage(GetImageRequest getImageRequest) {
        var imageData=imageService.getImage(getImageRequest.getUri(), getImageRequest.getFullImage());
        return ResponseEntity.ok(new GetImageResponse().image(imageData));
    }

    @Override
  public ResponseEntity<UploadImageResponse> uploadImage(UploadImageRequest request) {
    iamService.checkPermissionOfCurrentAccount(request.getType(), Set.of(Permission.UPDATE));
    var result = imageService.uploadImage(request.getResourceId(), request.getType(), request.getImage());
    return ResponseEntity.ok(new UploadImageResponse().result(result));
  }
}
