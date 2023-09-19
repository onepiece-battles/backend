package com.yichi.onepiece.file.controller;

import com.yichi.onepiece.api.FileApi;
import com.yichi.onepiece.file.service.FileService;
import com.yichi.onepiece.model.DownloadFileRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class FileController implements FileApi {
  private final FileService fileService;

  public FileController(FileService fileService) {
    this.fileService = fileService;
  }

  @Override
  public ResponseEntity<Resource> downloadFile(DownloadFileRequest downloadFileRequest) {
    // todo(yichi): add permission control here.
    Resource resource = fileService.downloadFile(downloadFileRequest.getId());
    if (resource==null) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    } else {
      return ResponseEntity.ok(resource);
    }
  }
}
