package com.yichi.onepiece.file.service;

import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.file.dto.FileAccess;
import com.yichi.onepiece.user.UserHelper;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FileService {
  private static final Logger logger = LoggerFactory.getLogger(FileService.class);

  private final FileObjectService fileObjectService;

  public FileService(FileObjectService fileObjectService) {
    this.fileObjectService = fileObjectService;
  }

  /**
   * Resolves a short-lived, read-only signed OSS URL for a stored file so the client downloads the
   * bytes directly from OSS. The app server no longer reads whole files into memory and base64-
   * encodes them (which spiked heap/native memory per download on a small host).
   */
  public FileAccess getFileAccess(UUID fileId) {
    var fileObject = fileObjectService.get(fileId);
    if (!UserHelper.hasAdminPermission()
        && !UserHelper.getCurrentUserId().equals(fileObject.getCreatorId())) {
      throw new PermissionDeniedException("Access Denied");
    }
    try {
      Date expiration = Date.from(Instant.now().plus(Duration.ofMinutes(15)));
      String url =
          AliCloudObjectStorage.signFileUrl(fileObject.getOssUri(), expiration).toExternalForm();
      return new FileAccess(
          url, fileObject.getFileName(), fileObject.getHttpMimeType(), expiration.getTime());
    } catch (FileStorageException e) {
      logger.error("error signing file url", e);
    }
    throw new RuntimeException("file access failed");
  }
}
