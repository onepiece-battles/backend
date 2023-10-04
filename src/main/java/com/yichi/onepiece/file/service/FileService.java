package com.yichi.onepiece.file.service;

import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.exception.FileStorageException;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class FileService {

  private final FileObjectService fileObjectService;

  public FileService(FileObjectService fileObjectService) {
    this.fileObjectService = fileObjectService;
  }

  public Resource downloadFile(UUID fileId) {
    var fileObject = fileObjectService.get(fileId);
    try {
      String uri = fileObject.getOssUri();
      var inputStream = AliCloudObjectStorage.getFileStream(uri);
      return new InputStreamResource(inputStream);
    } catch (FileStorageException e) {
      e.printStackTrace();
      return null;
    }
  }
}
