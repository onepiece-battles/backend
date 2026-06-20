package com.yichi.onepiece.data_export.controller;

import com.yichi.onepiece.api.DataExportApi;
import com.yichi.onepiece.data_export.service.DataExportService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.GeneratePdfRequest;
import com.yichi.onepiece.model.GeneratePdfResponse;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.user.UserHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.Set;

@Controller
public class DataExportController implements DataExportApi {
  private final DataExportService dataExportService;
  private final IamService iamService;

  public DataExportController(DataExportService dataExportService, IamService iamService) {
    this.dataExportService = dataExportService;
    this.iamService = iamService;
  }

  @Override
  public ResponseEntity<GeneratePdfResponse> generatePdf(GeneratePdfRequest generatePdfRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.BATTLE, Set.of(Permission.GET));
    var currentUser = UserHelper.getCurrentUserDetail();
    var fileId =
        dataExportService.generateBattleResultPdf(
            generatePdfRequest.getResourceId(),
            currentUser.getUsername(),
            generatePdfRequest.getLocale(),
            currentUser.getId(),
            currentUser.getLoginRole().toString());
    return ResponseEntity.ok(new GeneratePdfResponse().fileId(fileId));
  }
}
