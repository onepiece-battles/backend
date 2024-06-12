package com.yichi.onepiece.data_export.service;

import com.itextpdf.io.font.PdfEncodings;
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
import com.itextpdf.layout.properties.*;
import com.itextpdf.layout.renderer.DocumentRenderer;
import com.itextpdf.layout.renderer.DrawContext;
import com.lowagie.text.Row;
import com.itextpdf.kernel.colors.Color;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.battle.model.BattleProcessDetailEntity;
import com.yichi.onepiece.battle.model.BattleStatusDetailEntity;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.file.model.FileCategory;
import com.yichi.onepiece.file.model.FileObjectEntityBuilder;
import com.yichi.onepiece.file.service.FileObjectService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.BattleReportStage;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.OnepieceLocale;
import com.yichi.onepiece.model.UserRole;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.I18nHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;

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
  public void generateBattleResultPdf(UUID battleId, String userEmail, String locale, UUID userId, String roleName) {
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
      String fontPath = "/font/NotoSansCJKsc-Regular.otf";
      PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
      PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
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
      String starterName = locale.equals(OnepieceLocale.ZH_CN.getValue())?
              starterCharacter.getName().getZhCN():starterCharacter.getName().getEnUS();
      String receiverName = locale.equals(OnepieceLocale.ZH_CN.getValue())?
              receiverCharacter.getName().getZhCN():starterCharacter.getName().getEnUS();

      // Add space at the top of the table
      Paragraph space = new Paragraph("\n"); // Empty paragraph for space
      space.setHeight(180); // Set desired space before the table
      document.add(space);
      Table table = new Table(3);
      var starterImageCell = createImageCell(starterImage, imageWidth, imageHeight);
      var startNameCell = createTextCell(starterName,20, new DeviceRgb(16, 85, 154), font);
      var receiverImageCell = createImageCell(receiverImage, imageWidth, imageHeight);
      var receiverNameCell = createTextCell(receiverName,20, new DeviceRgb(16, 85, 154), font);
      var vsCell =  createTextCell("VS",30, new DeviceRgb(16, 85, 154), font);
      var emptyCell = createTextCell("",20, new DeviceRgb(16, 85, 154), font);

      table.addCell(starterImageCell)
              .addCell(vsCell)
              .addCell(receiverImageCell)
              .addCell(startNameCell)
              .addCell(emptyCell)
              .addCell(receiverNameCell);


      table.setHorizontalAlignment(HorizontalAlignment.CENTER);
      document.add(table);
      document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
      var leftAlign = true;
      int i = 0;
      for (; i < processList.size(); i++) {
        document.add(getBattleStatusBlock(leftAlign, statusList.get(i), locale, font));
        document.add(getBattleProcessBlock(processList.get(i), locale, font));
        leftAlign = !leftAlign;
      }
      document.add(getBattleStatusBlock(leftAlign, statusList.get(i), locale, font));
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

  private Cell createTextCell(String text, float fontSize, DeviceRgb fontColor, PdfFont font) {
    Cell cell = new Cell();
    cell.add(new Paragraph(text)
                    .setFont(font)
                    .setHeight(100)
                    .setWidth(100)
            .setFontSize(fontSize)
            .setFontColor(fontColor, 255)
           .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE));
    cell.setTextAlignment(TextAlignment.CENTER);
    cell.setBorder(Border.NO_BORDER);
    return cell;
  }

  private Cell createImageCell(Image image, float imageWidth, float imageHeight) {
    Cell cell = new Cell();
    cell.add(image.setWidth(imageWidth).setHeight(imageHeight));
    cell.setBorder(Border.NO_BORDER);
    return cell;
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

  private Paragraph getBattleStatusBlock(
          Boolean leftAlign, BattleStatusDetailEntity battleStatusDetailEntity, String locale, PdfFont font) {
    Paragraph paragraph = new Paragraph();
    paragraph.setFont(font);
    paragraph.add(String.format("%s %s \n",I18nHelper.getTextByLocal(
                    new I18nText().zhCN("回合").enUS("Round"),locale) ,
            battleStatusDetailEntity.getBattleStatusRoundNumber()));
//    paragraph.add(I18nHelper.getTextByLocal(battleStatusDetailEntity.getAttackSide(), locale));
    paragraph.add(String.format("%s %s \n",I18nHelper.getTextByLocal(
                    new I18nText().zhCN("进攻方当前血量").enUS("Attacker current blood"), locale),
            battleStatusDetailEntity.getAttacker().getBlood()));
    paragraph.add(String.format("%s %s \n",I18nHelper.getTextByLocal(
                    new I18nText().zhCN("进攻方当前能量").enUS("Attacker current energy"), locale),
            battleStatusDetailEntity.getAttacker().getCurrentEnergy()));
    paragraph.add(String.format("%s %s \n",I18nHelper.getTextByLocal(
                    new I18nText().zhCN("防守方当前血量").enUS("Defender current blood"), locale),
            battleStatusDetailEntity.getDefender().getBlood()));
    paragraph.add(String.format("%s %s \n",I18nHelper.getTextByLocal(
                    new I18nText().zhCN("防守方当前能量").enUS("Defender current energy"), locale),
            battleStatusDetailEntity.getDefender().getCurrentEnergy()));
    paragraph.setTextAlignment(leftAlign? TextAlignment.LEFT: TextAlignment.RIGHT)
            .setFontSize(12)
            .setFontColor(new DeviceRgb(16, 85, 154),255)
            .setHorizontalAlignment(leftAlign? HorizontalAlignment.LEFT: HorizontalAlignment.RIGHT);
    return paragraph;
  }

  private Paragraph getBattleProcessBlock(
          BattleProcessDetailEntity battleProcessDetail, String locale, PdfFont font) {
    Paragraph paragraph = new Paragraph();
    paragraph.setFont(font);
    if (battleProcessDetail.getAttackMissed()) {
      paragraph.add(String.format("%s \n",I18nHelper.getTextByLocal(
              new I18nText().zhCN("进攻被躲避").enUS("Attack missed"), locale)));
    }
    paragraph.add(String.format("%s %s \n",I18nHelper.getTextByLocal(
                    new I18nText().zhCN("进攻者能量损失").enUS("Attacker energy loss"), locale),
            battleProcessDetail.getAttackerEnergyLoss()));
    paragraph.add(String.format("%s %s \n",I18nHelper.getTextByLocal(
                    new I18nText().zhCN("防守者血量损失").enUS("Defender blood loss"), locale),
            battleProcessDetail.getDefenderBloodLoss()));
    paragraph.setTextAlignment(TextAlignment.CENTER)
            .setFontSize(12)
            .setFontColor(new DeviceRgb(16, 85, 154),255)
            .setHorizontalAlignment( HorizontalAlignment.CENTER);
    return paragraph;
  }
}
