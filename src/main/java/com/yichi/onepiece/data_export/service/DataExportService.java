package com.yichi.onepiece.data_export.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.colorspace.PdfColorSpace;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.Document;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.layout.renderer.DocumentRenderer;
import com.itextpdf.layout.renderer.DrawContext;
import com.lowagie.text.Row;
import com.itextpdf.kernel.colors.Color;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.file.model.FileCategory;
import com.yichi.onepiece.file.model.FileObjectEntityBuilder;
import com.yichi.onepiece.file.service.FileObjectService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.BattleReportStage;
import com.yichi.onepiece.model.UserRole;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import com.yichi.onepiece.user.service.UserService;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DataExportService {
  private final Logger logger = LoggerFactory.getLogger(DataExportService.class);

  private final BattleService battleService;
  private final CharacterService characterService;
  private final FileObjectService fileObjectService;
  private final ActionRecordService actionRecordService;
  private final UserService userService;

  public DataExportService(
          BattleService battleService,
          CharacterService characterService,
          FileObjectService fileObjectService, ActionRecordService actionRecordService,
          UserService userService) {
    this.battleService = battleService;
    this.characterService = characterService;
    this.fileObjectService = fileObjectService;
      this.actionRecordService = actionRecordService;
      this.userService = userService;
  }

  /**
   * Export pdf data to AliYun oss and return the path.
   *
   * @param battleId id of battle that will be analyzed.
   */
  public void generateBattleResultPdf(UUID battleId, String userEmail, String userNickname, UUID userId, String roleName) {
    logger.info("start getting source data for report generation");
    var battleEntity = battleService.findBattleById(battleId);
    var userEntity = userService.getUserEntityById(userId);
    var statusList = battleService.listBattleStatusDetailByBattleId(battleId);
    var processList = battleService.listBattleProcessDetailByBattleId(battleId);
    var starterCharacter = characterService.findByCharacterId(battleEntity.getStarterId());
    var receiverCharacter = characterService.findByCharacterId(battleEntity.getReceiverId());
    try {
      float imageWidth = 100;
      float imageHeight = 100;
      logger.info("start creating files");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PdfWriter writer = new PdfWriter(outputStream);
      PdfDocument pdfDocument = new PdfDocument(writer);
      Document document = new Document(pdfDocument);

      // title
      Paragraph title = new Paragraph("Onepiece Battles")
              .setTextAlignment(TextAlignment.CENTER)
              .setFontSize(24);
      document.add(title);
      pdfDocument.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE,
              new FooterEventHandler(document, userEmail));

      // create image
      ImageData starterImageData = ImageDataFactory.create(ArrayUtils.toPrimitive(starterCharacter.getProfileImage()));
      Image starterImage = new Image(starterImageData)
              .scaleToFit(100, 100).setHorizontalAlignment(HorizontalAlignment.CENTER);
      ImageData receiverImageData = ImageDataFactory.create(ArrayUtils.toPrimitive(receiverCharacter.getProfileImage()));
      Image receiverImage = new Image(receiverImageData)
              .scaleToFit(100, 100).setHorizontalAlignment(HorizontalAlignment.CENTER);
      String starterName = starterCharacter.getName().getZhCN();
      String receiverName = receiverCharacter.getName().getZhCN();

      // Add space at the top of the table
      Paragraph space = new Paragraph("\n"); // Empty paragraph for space
      space.setHeight(180); // Set desired space before the table
      document.add(space);

      Table table = new Table(3);
      Cell cell1 = new Cell();
      cell1.add(starterImage.setWidth(imageWidth).setHeight(imageHeight).setTextAlignment(TextAlignment.CENTER));
      cell1.add(new Paragraph(starterName).setTextAlignment(TextAlignment.CENTER)
              .setFontSize(20)
              .setFontColor(new DeviceRgb(16, 85, 154),255)
              .setVerticalAlignment(VerticalAlignment.BOTTOM));
      cell1.setBorder(Border.NO_BORDER);
      table.addCell(cell1);

      Cell cell3 = new Cell();
      cell3.add(new Paragraph("VS").setWidth(100)
                      .setFontSize(30)
                      .setFontColor(new DeviceRgb(16, 85, 154),255)
              .setHeight(imageHeight).setTextAlignment(TextAlignment.CENTER)
              .setVerticalAlignment(VerticalAlignment.MIDDLE));
      cell3.setTextAlignment(TextAlignment.CENTER);
      cell3.setBorder(Border.NO_BORDER);
      table.addCell(cell3);


      Cell cell2 = new Cell();
      cell2.add(receiverImage.setWidth(imageWidth).setHeight(imageHeight).setTextAlignment(TextAlignment.CENTER));
      cell2.add(new Paragraph(receiverName).setTextAlignment(TextAlignment.CENTER)
              .setFontSize(20)
              .setFontColor(new DeviceRgb(16, 85, 154),255)
              .setVerticalAlignment(VerticalAlignment.BOTTOM));
      cell2.setBorder(Border.NO_BORDER);
      table.addCell(cell2);

      table.setHorizontalAlignment(HorizontalAlignment.CENTER);
      document.add(table);
      document.setMargins(10, 20, 10, 20);
      document.close();
      writer.close();
      logger.info("start writing files to alicloud");
      String uri =
          AliCloudObjectStorage.putFileStream(new ByteArrayInputStream(outputStream.toByteArray()));
      logger.info("finish uploading to alicloud oss");
      var actionRecordEntity = actionRecordService.createWithUserAndRole(userEntity, UserRole.fromValue(roleName));
      // use en_US as detail component of file name
      var fileObjectEntity =
          FileObjectEntityBuilder.newBuilder()
              .setFileName(
                  getFileName(
                           starterCharacter.getName().getEnUS(), receiverCharacter.getName().getEnUS(), battleId))
              .setFileCategory(FileCategory.SINGLE_BATTLE_DETAIL)
              .setHttpMimeType("application/pdf")
              .setCreatorId(userId)
              .setCreationRecordId(actionRecordEntity.getId())
              .setOssUri(uri)
              .setResourceId(battleId)
              .build();
      fileObjectService.add(fileObjectEntity);
      logger.info("pdf report generating finish");
      // update battle entity
      battleService.updateBattleReportStage(battleId, BattleReportStage.FINISHED);
      battleService.updateFileId(battleId, fileObjectEntity.getId());
    } catch (Exception e) {
      logger.error(e.toString());
    }
  }

  private String getFileName(String starterCharacter, String receiverCharacter, UUID battleId) {
    return String.format(
        "%s_%s_%s",
             starterCharacter, receiverCharacter, battleId);
  }

  static class FooterEventHandler implements IEventHandler {
    private final Document document;
    private final String userEmail;

    public FooterEventHandler(Document document, String userEmail) {
      this.document = document;
      this.userEmail = userEmail;
    }

    @Override
    public void handleEvent(com.itextpdf.kernel.events.Event  event) {
      PdfPage page = ((PdfDocumentEvent) event).getPage();
      Rectangle pageSize = page.getPageSize();

      PdfCanvas canvas = new PdfCanvas(page.newContentStreamBefore(),
              page.getResources(), page.getDocument());

      canvas.moveTo(pageSize.getLeft(), 10)
              .lineTo(pageSize.getRight(), 10)
              .setLineWidth(0.5f)
              .stroke();

      int pageNumber = ((PdfDocumentEvent) event).getDocument().getPageNumber(page);
      // add footer
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
      LocalDateTime now = LocalDateTime.now();
      Paragraph footer = new Paragraph().setHeight(15);
      Text leftSpan = new Text("Generated by "+ userEmail).setFontSize(8).setTextAlignment(TextAlignment.LEFT);
      Text rightSpan = new Text(dtf.format(now)).setFontSize(8).setTextAlignment(TextAlignment.RIGHT);
      footer.add(leftSpan); // A
      for (int i = 0; i < 8; i ++){
        footer.add(new Tab());
      }
      footer.add(rightSpan);
      footer.setFontSize(8);
      footer.setFontColor(new DeviceRgb(95, 110, 121));
      footer.setTextAlignment(TextAlignment.CENTER);
      try {
          canvas.beginText()
                  .setFontAndSize(PdfFontFactory.createFont(), 8)
                  .moveText((pageSize.getLeft() + pageSize.getRight()) / 2, 2)
                  .showText(String.valueOf(pageNumber))
                  .endText();
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
      document.showTextAligned(footer, pageSize.getWidth() / 2, 5, pageNumber,
            TextAlignment.CENTER, VerticalAlignment.BOTTOM, 0);

    }
  }
}
