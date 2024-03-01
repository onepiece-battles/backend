package com.yichi.onepiece.file.service;

import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.exception.FileStorageException;

import java.io.IOException;
import java.util.UUID;

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

  public byte[] downloadFile(UUID fileId) {
    var fileObject = fileObjectService.get(fileId);
    try {
      String uri = fileObject.getOssUri();
      var inputStream = AliCloudObjectStorage.getFileStream(uri);
      return inputStream.readAllBytes();
    } catch (FileStorageException | IOException e) {
      logger.error("error downloading file", e);
    }
    return null;
  }
}
