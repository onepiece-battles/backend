package com.yichi.onepiece.data_export.controller;

import com.yichi.onepiece.api.DataExportApi;
import com.yichi.onepiece.data_export.service.DataExportService;
import com.yichi.onepiece.model.GeneratePdfRequest;
import com.yichi.onepiece.model.GeneratePdfResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class DataExportController implements DataExportApi {
  private final DataExportService dataExportService;

  public DataExportController(DataExportService dataExportService) {
    this.dataExportService = dataExportService;
  }

  @Override
  public ResponseEntity<GeneratePdfResponse> generatePdf(GeneratePdfRequest generatePdfRequest) {
    // todo(yichi): add permission check here
    dataExportService.generateBattleResultPdf(generatePdfRequest.getResourceId(),"","",UUID.randomUUID());
    return ResponseEntity.ok(new GeneratePdfResponse().fileId(UUID.randomUUID()));
  }
}
