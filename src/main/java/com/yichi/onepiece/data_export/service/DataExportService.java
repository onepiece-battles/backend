package com.yichi.onepiece.data_export.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.Document;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.*;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.battle.model.BattleProcessDetailEntity;
import com.yichi.onepiece.battle.model.BattleStatusDetailEntity;
import com.yichi.onepiece.battle.model.CurrentCharacterEntity;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.utils.I18nHelper;
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
  public void generateBattleResultPdf(UUID battleId, String userEmail, String locale, UUID userId, String roleName) {
    logger.info("start getting source data for report generation");
    var battleEntity = battleService.findBattleById(battleId);
    var battleExecutionEntity = battleService.getBattleExecution(battleId);
    var userEntity = userService.getUserEntityById(userId);
    var statusList = battleService.listBattleStatusDetailByBattleId(battleId);
    var processList = battleService.listBattleProcessDetailByBattleId(battleId);
    var starterCharacter = characterService.findByCharacterId(battleEntity.getStarterId());
    var receiverCharacter = characterService.findByCharacterId(battleEntity.getReceiverId());
    try {
      float imageWidth = 100;
      float imageHeight = 100;
      float smallImageWidth = 30;
      float smallImageHeight = 30;
      String fontPath = "/font/NotoSansCJKsc-Regular.otf";
      PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
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
      ImageData starterImageData = ImageDataFactory.create(AliCloudObjectStorage.getFileStream(starterCharacter.getImageUri()).readAllBytes());
      Image starterImage = new Image(starterImageData)
              .scaleToFit(imageWidth, imageHeight).setHorizontalAlignment(HorizontalAlignment.CENTER);
      ImageData receiverImageData = ImageDataFactory.create(AliCloudObjectStorage.getFileStream(receiverCharacter.getImageUri()).readAllBytes());
      Image receiverImage = new Image(receiverImageData)
              .scaleToFit(imageWidth, imageHeight).setHorizontalAlignment(HorizontalAlignment.CENTER);

      Image starterImageSmall = new Image(starterImageData)
              .scaleToFit(smallImageWidth, smallImageHeight).setHorizontalAlignment(HorizontalAlignment.CENTER);
      Image receiverImageSmall = new Image(receiverImageData)
              .scaleToFit(smallImageWidth, smallImageHeight).setHorizontalAlignment(HorizontalAlignment.CENTER);
      String starterName = locale.equals(OnepieceLocale.ZH_CN.getValue())?
              starterCharacter.getName().getZhCN():starterCharacter.getName().getEnUS();
      String receiverName = locale.equals(OnepieceLocale.ZH_CN.getValue())?
              receiverCharacter.getName().getZhCN():receiverCharacter.getName().getEnUS();

      // Add space at the top of the table
      Paragraph space = new Paragraph("\n"); // Empty paragraph for space
      space.setHeight(180); // Set desired space before the table
      document.add(space);
      Table table = new Table(3);
      var starterImageCell = createImageCell(starterImage, imageWidth, imageHeight);
      var startNameCell = createTextCell(starterName,20, new DeviceRgb(16, 85, 154), font, 100, 60);
      var receiverImageCell = createImageCell(receiverImage, imageWidth, imageHeight);
      var receiverNameCell = createTextCell(receiverName,20, new DeviceRgb(16, 85, 154), font, 100, 60);
      var vsCell =  createTextCell("VS",30, new DeviceRgb(16, 85, 154), font, 100, 60);
      var emptyCell = createTextCell("",20, new DeviceRgb(16, 85, 154), font);
      var winnerImageCell = battleExecutionEntity.getWinnerId().equals(battleEntity.getStarterId())?
              createImageCell(starterImage, imageWidth, imageHeight):
              createImageCell(receiverImage, imageWidth, imageHeight);
      var winnerCell =  createTextCell(locale.equals(OnepieceLocale.ZH_CN.getValue())?
              "获胜者":"WINNER",30, new DeviceRgb(16, 85, 154), font, 150, 60);

      var smallStarterImageCell = createImageCell(starterImageSmall, smallImageWidth, smallImageHeight);
      var smallReceiverImageCell = createImageCell(receiverImageSmall, smallImageWidth, smallImageHeight);

      table.addCell(starterImageCell)
              .addCell(vsCell)
              .addCell(receiverImageCell)
              .addCell(startNameCell)
              .addCell(emptyCell)
              .addCell(receiverNameCell)
              .addCell(createEmptyCell(100,100))
              .addCell(emptyCell)
              .addCell(emptyCell)
              .addCell(emptyCell)
              .addCell(winnerCell)
              .addCell(emptyCell)
              .addCell(emptyCell)
              .addCell(winnerImageCell)
              .addCell(emptyCell);

      table.setHorizontalAlignment(HorizontalAlignment.CENTER);
      document.add(table);
      document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
      document.add(new Paragraph(locale.equals(OnepieceLocale.ZH_CN.getValue())?
              "战斗详情":"BATTLE DETAILS").setFont(font).setFontSize(25))
              .setHorizontalAlignment(HorizontalAlignment.CENTER).setTextAlignment(TextAlignment.CENTER);
      int i = 0;
      for (; i < processList.size(); i++) {
        document.add(getBattleStatusBlock(smallStarterImageCell, smallReceiverImageCell, statusList.get(i), locale, font));
        document.add(getBattleProcessBlock(processList.get(i), locale, font, starterCharacter.getId()));
      }
      document.add(getBattleStatusBlock(smallStarterImageCell, smallReceiverImageCell, statusList.get(i), locale, font));
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

  private Cell createTextCell(String text,
                              float fontSize,
                              DeviceRgb fontColor,
                              PdfFont font) {
    Cell cell = new Cell();
    cell.add(new Paragraph(text)
                    .setFont(font)
                    .setHeight(30)
                    .setWidth(100)
            .setFontSize(fontSize)
            .setFontColor(fontColor, 255)
           .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE));
    cell.setTextAlignment(TextAlignment.LEFT);
    cell.setVerticalAlignment(VerticalAlignment.TOP);
    cell.setBorder(Border.NO_BORDER);
    return cell;
  }

  private Cell createTextCell(String text, float fontSize, DeviceRgb fontColor, PdfFont font, float width, float height) {
    Cell cell = new Cell();
    cell.add(new Paragraph(text)
            .setFont(font)
            .setHeight(height)
            .setWidth(width)
            .setFontSize(fontSize)
            .setFontColor(fontColor, 255)
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE));
    cell.setTextAlignment(TextAlignment.LEFT);
    cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
    cell.setBorder(Border.NO_BORDER);
    return cell;
  }

  private Cell createEmptyCell(float width) {
    return new Cell().setBorder(Border.NO_BORDER).setWidth(width);
  }

  private Cell createEmptyCell(float width, float height) {
    return new Cell().setBorder(Border.NO_BORDER).setWidth(width).setHeight(height);
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

  private Table getBattleStatusBlock(
          Cell starterImageCell,
          Cell receiverImageCell,
          BattleStatusDetailEntity battleStatusDetailEntity,
          String locale,
          PdfFont font) {
    Table table = new Table(6);
    CurrentCharacterEntity starter = battleStatusDetailEntity.getInitSide();
    CurrentCharacterEntity receiver = battleStatusDetailEntity.getReceiveSide();

    Paragraph starterBlood = new Paragraph(String.format("%s %s \n",I18nHelper.getTextByLocal(
                    new I18nText().zhCN("剩余血量").enUS("Blood left"), locale),
            starter.getBlood())).setFont(font)
            .setFontSize(8)
            .setTextAlignment(TextAlignment.LEFT);;
    Paragraph starterEnergy = new Paragraph(String.format("%s %s \n",I18nHelper.getTextByLocal(
                    new I18nText().zhCN("剩余能量").enUS("Energy left"), locale),
            starter.getCurrentEnergy())).setFont(font)
            .setFontSize(8)
            .setTextAlignment(TextAlignment.LEFT);;

    Paragraph receiverBlood = new Paragraph(String.format("%s %s \n",I18nHelper.getTextByLocal(
                    new I18nText().zhCN("剩余血量").enUS("Blood left"), locale), receiver.getBlood())).setFont(font)
            .setFontSize(8)
            .setTextAlignment(TextAlignment.RIGHT);;
    Paragraph receiverEnergy = new Paragraph(String.format("%s %s \n",I18nHelper.getTextByLocal(
                    new I18nText().zhCN("剩余能量").enUS("Energy left"), locale), receiver.getCurrentEnergy())).setFont(font)
            .setFontSize(8)
            .setTextAlignment(TextAlignment.RIGHT);;

    Cell starterCell = new Cell()
            .setHeight(30).setWidth(300)
            .setFont(font).setFontColor(new DeviceRgb(16, 85, 154)).setFontSize(10).setBorder(Border.NO_BORDER);
    starterCell.add(starterBlood);
    starterCell.add(starterEnergy);

    Cell receiverCell = new Cell()
            .setHeight(30).setWidth(300)
            .setFont(font).setFontColor(new DeviceRgb(16, 85, 154)).setFontSize(10).setBorder(Border.NO_BORDER);
    receiverCell.add(receiverBlood);
    receiverCell.add(receiverEnergy);


    table.addCell(starterImageCell)
            .addCell(starterCell)
            .addCell(createEmptyCell(200))
            .addCell(createEmptyCell(200))
            .addCell(receiverCell)
            .addCell(receiverImageCell);
    table.setTextAlignment(TextAlignment.CENTER);
    return table;
  }

  private Paragraph getBattleProcessBlock(
          BattleProcessDetailEntity battleProcessDetail, String locale, PdfFont font, UUID starterId) {
    Paragraph paragraph = new Paragraph();
    paragraph.setFont(font);
    paragraph.add(String.format("%s \n",I18nHelper.getTextByLocal(
            new I18nText().zhCN(String.format("第 %s 回合", battleProcessDetail.getBattleProcessRoundNumber()))
                    .enUS(String.format("Round %s", battleProcessDetail.getBattleProcessRoundNumber())), locale)));
    if (battleProcessDetail.getAttackerId().equals(starterId)) {
      paragraph.add(String.format("%s \n",I18nHelper.getTextByLocal(
              new I18nText().zhCN(">>> 进攻 >>>").enUS(">>> ATTACK >>>"), locale)));
    } else {
      paragraph.add(String.format("%s \n",I18nHelper.getTextByLocal(
              new I18nText().zhCN("<<< 进攻 <<<").enUS("<<< ATTACK <<<"), locale)));
    }
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
