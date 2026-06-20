package com.yichi.onepiece.file.controller;

import com.aliyuncs.utils.Base64Helper;
import com.yichi.onepiece.api.FileApi;
import com.yichi.onepiece.file.service.FileService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.DownloadFileRequest;
import com.yichi.onepiece.model.DownloadFileResponse;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.Set;

@Controller
public class FileController implements FileApi {
  private final FileService fileService;
  private final IamService iamService;

  public FileController(FileService fileService, IamService iamService) {
    this.fileService = fileService;
    this.iamService = iamService;
  }

  @Override
  public ResponseEntity<DownloadFileResponse> downloadFile(DownloadFileRequest downloadFileRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.BATTLE, Set.of(Permission.GET));
    var response = fileService.downloadFile(downloadFileRequest.getId());
    return ResponseEntity.ok(response);
  }
}
