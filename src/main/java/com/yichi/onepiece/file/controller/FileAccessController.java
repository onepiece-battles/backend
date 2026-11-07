package com.yichi.onepiece.file.controller;

import com.yichi.onepiece.file.dto.FileAccess;
import com.yichi.onepiece.file.service.FileService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Returns a signed OSS URL for a stored file (e.g. a generated battle-report PDF) so the browser
 * downloads it directly from OSS. JWT authentication is enforced by the same /api security chain.
 */
@RestController
public class FileAccessController {
  private final FileService fileService;
  private final IamService iamService;

  public FileAccessController(FileService fileService, IamService iamService) {
    this.fileService = fileService;
    this.iamService = iamService;
  }

  @PostMapping("/api/onepiece/file/get_file_url")
  public ResponseEntity<FileAccess> getFileUrl(@RequestBody FileUrlRequest request) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.BATTLE, Set.of(Permission.GET));
    var access = fileService.getFileAccess(request.id());
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(access);
  }

  public record FileUrlRequest(UUID id) {}
}
