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
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.properties.*;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.battle.model.BattleProcessDetailEntity;
import com.yichi.onepiece.battle.model.BattleStatusDetailEntity;
import com.yichi.onepiece.battle.model.CurrentCharacterEntity;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.file.model.FileCategory;
import com.yichi.onepiece.file.model.FileObjectEntityBuilder;
import com.yichi.onepiece.file.service.FileObjectService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.BattleExecution;
import com.yichi.onepiece.model.BattleReportStage;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.OnepieceLocale;
import com.yichi.onepiece.model.UserRole;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javax.imageio.ImageIO;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.utils.I18nHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DataExportService {
  private final Logger logger = LoggerFactory.getLogger(DataExportService.class);
  private static final DeviceRgb NAVY = new DeviceRgb(18, 52, 88);
  private static final DeviceRgb BLUE = new DeviceRgb(32, 112, 180);
  private static final DeviceRgb LIGHT_BLUE = new DeviceRgb(230, 241, 252);
  private static final DeviceRgb GOLD = new DeviceRgb(235, 168, 52);
  private static final DeviceRgb RED = new DeviceRgb(194, 73, 73);
  private static final DeviceRgb GREEN = new DeviceRgb(42, 141, 103);
  private static final DeviceRgb GRAY = new DeviceRgb(91, 103, 112);
  private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(246, 248, 250);
  private static final DeviceRgb BORDER = new DeviceRgb(214, 224, 234);

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
  public UUID generateBattleResultPdf(UUID battleId, String userEmail, String locale, UUID userId, String roleName) {
    try {
      logger.info("start getting source data for report generation");
      var battleEntity = battleService.findBattleById(battleId);
      var battleExecutionEntity = battleService.getBattleExecution(battleId);
      var userEntity = userService.getUserEntityById(userId);
      var statusList = battleService.listBattleStatusDetailByBattleId(battleId);
      var processList = battleService.listBattleProcessDetailByBattleId(battleId);
      var starterCharacter = characterService.findByCharacterId(battleEntity.getStarterId());
      var receiverCharacter = characterService.findByCharacterId(battleEntity.getReceiverId());
      String fontPath = "/font/NotoSansCJKsc-Regular.otf";
      PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H);
      logger.info("start creating files");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      PdfWriter writer = new PdfWriter(outputStream);
      PdfDocument pdfDocument = new PdfDocument(writer);
      Document document = new Document(pdfDocument);
      document.setMargins(46, 42, 38, 42);

      pdfDocument.addEventHandler(com.itextpdf.kernel.events.PdfDocumentEvent.END_PAGE,
              new FooterEventHandler(document, userEmail));

      String starterName = locale.equals(OnepieceLocale.ZH_CN.getValue())?
              starterCharacter.getName().getZhCN():starterCharacter.getName().getEnUS();
      String receiverName = locale.equals(OnepieceLocale.ZH_CN.getValue())?
              receiverCharacter.getName().getZhCN():receiverCharacter.getName().getEnUS();
      ImageData starterImageData = loadCharacterImage(starterCharacter, starterName);
      ImageData receiverImageData = loadCharacterImage(receiverCharacter, receiverName);
      boolean starterWon = battleEntity.getStarterId().equals(battleExecutionEntity.getWinnerId());
      String winnerName = starterWon ? starterName : receiverName;

      Div overviewSection = new Div().setKeepTogether(true);
      overviewSection.add(createCoverHeader(font, locale, battleId, winnerName));
      overviewSection.add(createFighterSection(
              starterCharacter,
              receiverCharacter,
              starterImageData,
              receiverImageData,
              starterName,
              receiverName,
              starterWon,
              battleExecutionEntity,
              font,
              locale));
      overviewSection.add(createScoreSummary(battleExecutionEntity, starterName, receiverName, font, locale));
      document.add(overviewSection);

      document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
      document.add(sectionTitle(localized("战斗详情", "Battle Details", locale), font));
      int i = 0;
      for (; i < processList.size(); i++) {
        document.add(getBattleStatusBlock(starterImageData, receiverImageData, statusList.get(i), locale, font));
        document.add(getBattleProcessBlock(processList.get(i), locale, font, starterCharacter.getId()));
      }
      if (!statusList.isEmpty() && i < statusList.size()) {
        document.add(getBattleStatusBlock(starterImageData, receiverImageData, statusList.get(i), locale, font));
      }
      document.add(new Paragraph(localized("战斗结束", "Battle finished", locale))
              .setFont(font)
              .setFontSize(13)
              .setFontColor(GREEN)
              .setBold()
              .setTextAlignment(TextAlignment.CENTER)
              .setMarginTop(14));
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
      return fileObjectEntity.getId();
    } catch (Exception e) {
      logger.error("failed to generate battle report for battle {}", battleId, e);
      try {
        battleService.updateBattleReportStage(battleId, BattleReportStage.WAITING);
      } catch (Exception updateException) {
        logger.error("failed to reset battle report stage for battle {}", battleId, updateException);
      }
    }
    return null;
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

  private ImageData loadCharacterImage(CharacterEntity character, String fallbackName) {
    if (character.getImageUri() != null && !character.getImageUri().isBlank()) {
      try {
        return ImageDataFactory.create(
                AliCloudObjectStorage.getFileStream(character.getImageUri()).readAllBytes());
      } catch (Exception e) {
        logger.warn(
                "failed to load pdf image from OSS for character {}, using fallback image",
                character.getId(),
                e);
      }
    }
    if (character.getProfileImage() != null && character.getProfileImage().length > 0) {
      try {
        byte[] imageBytes = new byte[character.getProfileImage().length];
        for (int i = 0; i < character.getProfileImage().length; i++) {
          imageBytes[i] = character.getProfileImage()[i];
        }
        return ImageDataFactory.create(imageBytes);
      } catch (Exception e) {
        logger.warn(
                "failed to load pdf image from database profile for character {}, using fallback image",
                character.getId(),
                e);
      }
    }
    return createPlaceholderImage(fallbackName);
  }

  private ImageData createPlaceholderImage(String name) {
    try {
      int size = 240;
      BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = image.createGraphics();
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setColor(new Color(230, 241, 252));
      graphics.fillRect(0, 0, size, size);
      graphics.setColor(new Color(18, 52, 88));
      graphics.fillOval(28, 28, size - 56, size - 56);
      graphics.setColor(new Color(235, 168, 52));
      graphics.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 64));
      String initials = getInitials(name);
      int textWidth = graphics.getFontMetrics().stringWidth(initials);
      graphics.drawString(initials, (size - textWidth) / 2, 142);
      graphics.dispose();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ImageIO.write(image, "png", outputStream);
      return ImageDataFactory.create(outputStream.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException("failed to create placeholder image", e);
    }
  }

  private String getInitials(String name) {
    if (name == null || name.isBlank()) {
      return "?";
    }
    String[] parts = name.trim().split("\\s+");
    if (parts.length == 1) {
      return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
    }
    return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
  }

  private Div createCoverHeader(PdfFont font, String locale, UUID battleId, String winnerName) {
    Div cover = new Div()
            .setBackgroundColor(NAVY)
            .setPadding(18)
            .setMarginBottom(12);
    cover.add(new Paragraph("ONEPIECE BATTLES")
            .setFont(font)
            .setFontSize(11)
            .setFontColor(GOLD)
            .setBold()
            .setCharacterSpacing(1.3f)
            .setMarginBottom(4));
    cover.add(new Paragraph(localized("战斗报告", "Battle Report", locale))
            .setFont(font)
            .setFontSize(25)
            .setFontColor(new DeviceRgb(255, 255, 255))
            .setBold()
            .setMarginBottom(8));
    cover.add(new Paragraph(String.format("%s: %s", localized("获胜者", "Winner", locale), winnerName))
            .setFont(font)
            .setFontSize(13)
            .setFontColor(new DeviceRgb(226, 238, 249))
            .setMarginBottom(4));
    cover.add(new Paragraph(String.format("%s: %s", localized("战斗编号", "Battle ID", locale), battleId))
            .setFont(font)
            .setFontSize(8)
            .setFontColor(new DeviceRgb(185, 205, 222)));
    return cover;
  }

  private Table createFighterSection(
          CharacterEntity starter,
          CharacterEntity receiver,
          ImageData starterImageData,
          ImageData receiverImageData,
          String starterName,
          String receiverName,
          boolean starterWon,
          BattleExecution execution,
          PdfFont font,
          String locale) {
    Table table = new Table(UnitValue.createPercentArray(new float[] {43, 14, 43}))
            .useAllAvailableWidth()
            .setMarginBottom(10)
            .setKeepTogether(true);
    table.addCell(createFighterCard(
            starter,
            starterImageData,
            starterName,
            starterWon,
            execution.getStarterScoreBefore(),
            execution.getStarterScoreAfter(),
            font,
            locale));
    table.addCell(new Cell()
            .setBorder(Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setTextAlignment(TextAlignment.CENTER)
            .add(new Paragraph("VS").setFont(font).setFontSize(24).setBold().setFontColor(GOLD)));
    table.addCell(createFighterCard(
            receiver,
            receiverImageData,
            receiverName,
            !starterWon,
            execution.getReceiverScoreBefore(),
            execution.getReceiverScoreAfter(),
            font,
            locale));
    return table;
  }

  private Cell createFighterCard(
          CharacterEntity character,
          ImageData imageData,
          String name,
          boolean winner,
          Integer scoreBefore,
          Integer scoreAfter,
          PdfFont font,
          String locale) {
    Cell cell = cardCell().setPadding(10);
    cell.add(new Image(imageData).scaleToFit(96, 96).setHorizontalAlignment(HorizontalAlignment.CENTER));
    cell.add(new Paragraph(name)
            .setFont(font)
            .setFontSize(14)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(NAVY)
            .setMarginTop(5)
            .setMarginBottom(2));
    cell.add(new Paragraph(winner ? localized("胜利者", "Winner", locale) : localized("挑战者", "Contender", locale))
            .setFont(font)
            .setFontSize(9)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(winner ? GREEN : GRAY)
            .setMarginBottom(6));
    cell.add(statsGrid(character, font, locale));
    cell.add(scoreFlow(localized("积分", "Score", locale), scoreBefore, scoreAfter, font));
    return cell;
  }

  private Table statsGrid(CharacterEntity character, PdfFont font, String locale) {
    Table table = new Table(UnitValue.createPercentArray(new float[] {50, 50})).useAllAvailableWidth();
    table.addCell(statCell(localized("攻击", "Attack", locale), character.getAttack(), font));
    table.addCell(statCell(localized("防御", "Defense", locale), character.getDefense(), font));
    table.addCell(statCell(localized("速度", "Speed", locale), character.getSpeed(), font));
    table.addCell(statCell(localized("血量", "Blood", locale), character.getBlood(), font));
    table.addCell(statCell(localized("智力", "Intelligence", locale), character.getIntelligence(), font));
    table.addCell(statCell(localized("能量", "Energy", locale), character.getEnergy(), font));
    return table;
  }

  private Cell statCell(String label, Integer value, PdfFont font) {
    return new Cell()
            .setBorder(Border.NO_BORDER)
            .setBackgroundColor(LIGHT_GRAY)
            .setPadding(4)
            .setMargin(2)
            .add(new Paragraph(label).setFont(font).setFontSize(6).setFontColor(GRAY).setMarginBottom(0))
            .add(new Paragraph(String.valueOf(safeInt(value))).setFont(font).setFontSize(10).setBold().setFontColor(NAVY));
  }

  private Table createScoreSummary(BattleExecution execution, String starterName, String receiverName, PdfFont font, String locale) {
    Table table = new Table(UnitValue.createPercentArray(new float[] {33, 33, 34}))
            .useAllAvailableWidth()
            .setMarginTop(6)
            .setKeepTogether(true);
    table.addCell(scoreChangeMetric(localized("胜者积分变化", "Winner score change", locale), execution.getWinnerScoreChange(), GREEN, font));
    table.addCell(scoreChangeMetric(localized("败者积分变化", "Loser score change", locale), execution.getLoserScoreChange(), RED, font));
    table.addCell(summaryMetric(
            localized("对阵", "Matchup", locale),
            String.format("%s / %s", starterName, receiverName),
            BLUE,
            font));
    return table;
  }

  private Cell summaryMetric(String label, String value, DeviceRgb color, PdfFont font) {
    return cardCell()
            .setPadding(12)
            .add(new Paragraph(label).setFont(font).setFontSize(9).setFontColor(GRAY).setMarginBottom(3))
            .add(new Paragraph(value).setFont(font).setFontSize(15).setBold().setFontColor(color));
  }

  private Cell scoreChangeMetric(String label, Integer value, DeviceRgb color, PdfFont font) {
    String arrow = safeInt(value) >= 0 ? "▲" : "▼";
    Div badge = new Div()
            .setBackgroundColor(LIGHT_GRAY)
            .setPadding(8)
            .setMarginTop(4);
    badge.add(new Paragraph()
            .add(new Text(arrow + " ").setFont(font).setFontSize(14).setFontColor(color).setBold())
            .add(new Text(signed(value)).setFont(font).setFontSize(24).setFontColor(color).setBold())
            .setMargin(0)
            .setTextAlignment(TextAlignment.CENTER));
    return cardCell()
            .setPadding(12)
            .add(new Paragraph(label).setFont(font).setFontSize(9).setFontColor(GRAY).setMarginBottom(2))
            .add(badge);
  }

  private Paragraph scoreFlow(String label, Integer before, Integer after, PdfFont font) {
    return new Paragraph()
            .add(new Text(label + ": ").setFont(font).setFontSize(9).setFontColor(GRAY))
            .add(new Text(String.valueOf(safeInt(before))).setFont(font).setFontSize(11).setFontColor(NAVY).setBold())
            .add(new Text("  →  ").setFont(font).setFontSize(13).setFontColor(GOLD).setBold())
            .add(new Text(String.valueOf(safeInt(after))).setFont(font).setFontSize(11).setFontColor(BLUE).setBold())
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(10);
  }

  private Paragraph sectionTitle(String title, PdfFont font) {
    return new Paragraph(title)
            .setFont(font)
            .setFontSize(21)
            .setBold()
            .setFontColor(NAVY)
            .setMarginBottom(14)
            .setTextAlignment(TextAlignment.CENTER);
  }

  private Cell cardCell() {
    return new Cell()
            .setBorder(new SolidBorder(BORDER, 0.8f))
            .setBackgroundColor(new DeviceRgb(255, 255, 255));
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

      float footerY = pageSize.getBottom() + 24;
      float left = pageSize.getLeft() + 42;
      float right = pageSize.getRight() - 42;

      canvas.moveTo(left, footerY + 14)
              .lineTo(right, footerY + 14)
              .setLineWidth(0.5f)
              .setStrokeColor(new DeviceRgb(214, 224, 234))
              .stroke();

      int pageNumber = ((PdfDocumentEvent) event).getDocument().getPageNumber(page);
      DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
      LocalDateTime now = LocalDateTime.now();
      try {
        PdfFont footerFont = PdfFontFactory.createFont();
        Table footer = new Table(UnitValue.createPercentArray(new float[] {42, 16, 42}))
                .setWidth(right - left)
                .setFixedLayout();
        footer.addCell(footerCell("Generated by " + safeFooterText(userEmail), TextAlignment.LEFT, footerFont));
        footer.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .add(new Paragraph(String.valueOf(pageNumber))
                        .setFont(footerFont)
                        .setFontSize(8)
                        .setBold()
                        .setFontColor(new DeviceRgb(255, 255, 255))
                        .setBackgroundColor(new DeviceRgb(18, 52, 88))
                        .setPaddingLeft(8)
                        .setPaddingRight(8)
                        .setPaddingTop(3)
                        .setPaddingBottom(3)));
        footer.addCell(footerCell(dtf.format(now), TextAlignment.RIGHT, footerFont));
        document.add(footer.setFixedPosition(pageNumber, left, footerY - 4, right - left));
      } catch (IOException e) {
          throw new RuntimeException(e);
      }

    }

    private Cell footerCell(String text, TextAlignment alignment, PdfFont font) {
      return new Cell()
              .setBorder(Border.NO_BORDER)
              .setTextAlignment(alignment)
              .setVerticalAlignment(VerticalAlignment.MIDDLE)
              .add(new Paragraph(text)
                      .setFont(font)
                      .setFontSize(8)
                      .setFontColor(new DeviceRgb(95, 110, 121)));
    }

    private String safeFooterText(String text) {
      return text == null || text.isBlank() ? "Onepiece Battles" : text;
    }
  }

  private Table getBattleStatusBlock(
          ImageData starterImageData,
          ImageData receiverImageData,
          BattleStatusDetailEntity battleStatusDetailEntity,
          String locale,
          PdfFont font) {
    Table table = new Table(UnitValue.createPercentArray(new float[] {9, 36, 10, 36, 9}))
            .useAllAvailableWidth()
            .setMarginTop(8)
            .setMarginBottom(4);
    CurrentCharacterEntity starter = battleStatusDetailEntity.getInitSide();
    CurrentCharacterEntity receiver = battleStatusDetailEntity.getReceiveSide();

    table.addCell(roundImageCell(starterImageData));
    table.addCell(statusCell(starter, TextAlignment.LEFT, font, locale));
    table.addCell(new Cell()
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .add(new Paragraph(String.format("#%s", battleStatusDetailEntity.getBattleStatusRoundNumber()))
                    .setFont(font).setFontSize(9).setBold().setFontColor(GOLD)));
    table.addCell(statusCell(receiver, TextAlignment.RIGHT, font, locale));
    table.addCell(roundImageCell(receiverImageData));
    table.setTextAlignment(TextAlignment.CENTER);
    return table;
  }

  private Div getBattleProcessBlock(
          BattleProcessDetailEntity battleProcessDetail, String locale, PdfFont font, UUID starterId) {
    boolean starterAttack = battleProcessDetail.getAttackerId().equals(starterId);
    boolean missed = Boolean.TRUE.equals(battleProcessDetail.getAttackMissed());
    Div card = new Div()
            .setBorder(new SolidBorder(missed ? GOLD : BORDER, 0.8f))
            .setBackgroundColor(missed ? new DeviceRgb(255, 249, 232) : LIGHT_BLUE)
            .setPadding(12)
            .setMarginBottom(10);

    Table header = new Table(UnitValue.createPercentArray(new float[] {55, 45})).useAllAvailableWidth();
    header.addCell(new Cell()
            .setBorder(Border.NO_BORDER)
            .add(new Paragraph(localized(
                    String.format("第 %s 回合", battleProcessDetail.getBattleProcessRoundNumber()),
                    String.format("Round %s", battleProcessDetail.getBattleProcessRoundNumber()),
                    locale))
                    .setFont(font).setFontSize(12).setBold().setFontColor(NAVY)));
    header.addCell(new Cell()
            .setBorder(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT)
            .add(attackBadge(starterAttack, font, locale)));
    card.add(header);

    Table metrics = new Table(UnitValue.createPercentArray(new float[] {33, 34, 33})).useAllAvailableWidth();
    metrics.addCell(roundMetric(localized("进攻者能量损失", "Attacker energy loss", locale), safeInt(battleProcessDetail.getAttackerEnergyLoss()), RED, font));
    metrics.addCell(roundMetric(localized("防守者血量损失", "Defender blood loss", locale), safeInt(battleProcessDetail.getDefenderBloodLoss()), RED, font));
    metrics.addCell(roundMetric(
            missed ? localized("结果", "Result", locale) : localized("防守者能量损失", "Defender energy loss", locale),
            missed ? localized("闪避", "Missed", locale) : String.valueOf(safeInt(battleProcessDetail.getDefenderEnergyLoss()) == 0 ? 2 : safeInt(battleProcessDetail.getDefenderEnergyLoss())),
            missed ? GOLD : RED,
            font));
    card.add(metrics);
    if (battleProcessDetail.getAttackSkill() != null && !battleProcessDetail.getAttackSkill().isBlank()) {
      card.add(new Paragraph(String.format("%s: %s", localized("技能", "Skill", locale), battleProcessDetail.getAttackSkill()))
              .setFont(font)
              .setFontSize(8)
              .setFontColor(GRAY)
              .setMarginTop(6));
    }
    return card;
  }

  private Cell roundImageCell(ImageData imageData) {
    return new Cell()
            .setBorder(Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .add(new Image(imageData).scaleToFit(34, 34));
  }

  private Cell statusCell(CurrentCharacterEntity character, TextAlignment alignment, PdfFont font, String locale) {
    return new Cell()
            .setBorder(Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setTextAlignment(alignment)
            .add(new Paragraph(String.format("%s: %s", localized("剩余血量", "Blood left", locale), safeInt(character.getBlood())))
                    .setFont(font).setFontSize(8).setFontColor(NAVY).setMarginBottom(1))
            .add(new Paragraph(String.format("%s: %s", localized("剩余能量", "Energy left", locale), safeInt(character.getCurrentEnergy())))
                    .setFont(font).setFontSize(8).setFontColor(GRAY).setMarginTop(0));
  }

  private Cell roundMetric(String label, Object value, DeviceRgb color, PdfFont font) {
    return new Cell()
            .setBorder(Border.NO_BORDER)
            .setPadding(6)
            .add(new Paragraph(label).setFont(font).setFontSize(7).setFontColor(GRAY).setMarginBottom(2))
            .add(new Paragraph(String.valueOf(value)).setFont(font).setFontSize(12).setBold().setFontColor(color));
  }

  private Paragraph attackBadge(boolean starterAttack, PdfFont font, String locale) {
    String attackText = localized("进攻", "Attack", locale);
    String text = starterAttack ? attackText + "  →" : "←  " + attackText;
    return new Paragraph(text)
            .setFont(font)
            .setFontSize(10)
            .setBold()
            .setFontColor(new DeviceRgb(255, 255, 255))
            .setBackgroundColor(BLUE)
            .setPaddingLeft(9)
            .setPaddingRight(9)
            .setPaddingTop(4)
            .setPaddingBottom(5)
            .setTextAlignment(TextAlignment.CENTER);
  }

  private String localized(String zhCN, String enUS, String locale) {
    return I18nHelper.getTextByLocal(new I18nText().zhCN(zhCN).enUS(enUS), locale);
  }

  private int safeInt(Integer value) {
    return value == null ? 0 : value;
  }

  private String signed(Integer value) {
    int safeValue = safeInt(value);
    return safeValue > 0 ? "+" + safeValue : String.valueOf(safeValue);
  }
}
