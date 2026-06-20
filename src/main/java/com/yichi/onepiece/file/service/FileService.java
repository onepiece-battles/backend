package com.yichi.onepiece.file.service;

import com.aliyuncs.utils.Base64Helper;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.exception.PermissionDeniedException;

import java.io.IOException;
import java.util.UUID;

import com.yichi.onepiece.model.DownloadFileResponse;
import com.yichi.onepiece.user.UserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class FileService {
  private static final Logger logger = LoggerFactory.getLogger(AliCloudObjectStorage.class);

  private final FileObjectService fileObjectService;

  public FileService(FileObjectService fileObjectService) {
    this.fileObjectService = fileObjectService;
  }

  public DownloadFileResponse downloadFile(UUID fileId) {
    var fileObject = fileObjectService.get(fileId);
    if (!UserHelper.hasAdminPermission() && !UserHelper.getCurrentUserId().equals(fileObject.getCreatorId())) {
      throw new PermissionDeniedException("Access Denied");
    }
    try {
      String uri = fileObject.getOssUri();
      var inputStream = AliCloudObjectStorage.getFileStream(uri);
      var response = new DownloadFileResponse();
      response.fileData(Base64Helper.encode(inputStream.readAllBytes()))
              .fileName(fileObject.getFileName())
              .httpMimeType(fileObject.getHttpMimeType());
      return response;
    } catch (FileStorageException | IOException e) {
      logger.error("error downloading file", e);
    }
    throw new RuntimeException("file retrieval failed");
  }
}
