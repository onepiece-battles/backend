package com.yichi.onepiece.file.controller;

import com.aliyuncs.utils.Base64Helper;
import com.yichi.onepiece.api.FileApi;
import com.yichi.onepiece.file.service.FileService;
import com.yichi.onepiece.model.DownloadFileRequest;
import com.yichi.onepiece.model.DownloadFileResponse;
import org.springframework.core.io.ByteArrayResource;
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
  public ResponseEntity<DownloadFileResponse> downloadFile(DownloadFileRequest downloadFileRequest) {
    // todo(yichi): add permission control here.
    var response = fileService.downloadFile(downloadFileRequest.getId());
    return ResponseEntity.ok(response);
  }
}
