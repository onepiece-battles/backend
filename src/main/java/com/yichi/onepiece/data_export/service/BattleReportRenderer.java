package com.yichi.onepiece.data_export.service;

import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.CompressionConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.yichi.onepiece.battle.model.BattleProcessDetailEntity;
import com.yichi.onepiece.battle.model.BattleStatusDetailEntity;
import com.yichi.onepiece.battle.model.CurrentCharacterEntity;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.model.BattleExecution;
import com.yichi.onepiece.model.OnepieceLocale;
import java.io.IOException;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class BattleReportRenderer {
  private static final DeviceRgb INK = new DeviceRgb(27, 45, 64);
  private static final DeviceRgb MUTED = new DeviceRgb(105, 119, 132);
  private static final DeviceRgb LINE = new DeviceRgb(222, 229, 235);
  private static final DeviceRgb STRIPE = new DeviceRgb(246, 248, 250);
  private static final DeviceRgb BLUE = new DeviceRgb(40, 101, 160);
  private static final DeviceRgb GREEN = new DeviceRgb(30, 122, 88);
  private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
  private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

  record Data(UUID battleId, BattleExecution execution, CharacterEntity starter, CharacterEntity receiver,
      List<BattleStatusDetailEntity> statuses, List<BattleProcessDetailEntity> processes,
      String locale, String generatedBy) {}

  void render(OutputStream output, Data data, ImageData starterImage, ImageData receiverImage) throws IOException {
    var properties = new WriterProperties().setFullCompressionMode(true)
        .setCompressionLevel(CompressionConstants.DEFAULT_COMPRESSION);
    var writer = new PdfWriter(output, properties);
    writer.setCloseStream(false);
    try (var pdf = new PdfDocument(writer); var document = new Document(pdf, PageSize.A4)) {
      PdfFont font = asciiOnly(data) ? PdfFontFactory.createFont(StandardFonts.HELVETICA)
          : PdfFontFactory.createFont(FontProgramFactory.createFont("/font/NotoSansCJKsc-Regular.otf", true),
              PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
      font.setSubset(true);
      document.setMargins(32, 36, 42, 36);
      document.setFont(font).setFontSize(9).setFontColor(INK);
      pdf.getDocumentInfo().setTitle(label(data, "战斗报告", "Battle Report"))
          .setCreator("Onepiece Battles");
      pdf.addEventHandler(PdfDocumentEvent.END_PAGE, new Footer(font, data.generatedBy()));

      var statuses = new HashMap<Integer, BattleStatusDetailEntity>();
      for (var status : data.statuses()) {
        if (status.getBattleStatusRoundNumber() != null) {
          statuses.put(status.getBattleStatusRoundNumber(), status);
        }
      }
      document.add(new Paragraph("ONEPIECE BATTLES").setFontSize(8).setCharacterSpacing(1.1f)
          .setFontColor(MUTED).setMarginBottom(2));
      document.add(new Paragraph(label(data, "战斗报告", "Battle Report")).setFontSize(23).setBold()
          .setMarginTop(0).setMarginBottom(3));
      document.add(new Paragraph(label(data, "战斗编号  ", "Battle ID  ") + data.battleId())
          .setFontSize(7).setFontColor(MUTED).setMarginBottom(12));
      String winner = winnerName(data);
      document.add(new Paragraph((winner == null ? label(data, "未记录胜者", "Winner not recorded")
          : label(data, "胜者  ", "Winner  ") + winner)
          + "   /   " + data.processes().size() + label(data, " 回合", data.processes().size() == 1 ? " round" : " rounds"))
          .setFontSize(12).setBold().setFontColor(GREEN).setBackgroundColor(new DeviceRgb(237, 247, 242))
          .setPadding(9).setMarginBottom(10));
      Table fighters = new Table(UnitValue.createPercentArray(new float[] {50, 50})).useAllAvailableWidth()
          .setKeepTogether(true).setMarginBottom(8);
      fighters.addCell(fighter(data, data.starter(), starterImage, "A", data.execution().getStarterScoreBefore(),
          data.execution().getStarterScoreAfter()));
      fighters.addCell(fighter(data, data.receiver(), receiverImage, "B", data.execution().getReceiverScoreBefore(),
          data.execution().getReceiverScoreAfter()));
      document.add(fighters);
      var firstStatus = statuses.entrySet().stream().min(Map.Entry.comparingByKey()).map(Map.Entry::getValue).orElse(null);
      var lastStatus = statuses.entrySet().stream().max(Map.Entry.comparingByKey()).map(Map.Entry::getValue).orElse(null);
      document.add(new Paragraph(label(data, "起始记录（生命 / 能量）", "First recorded state (HP / energy)")
          + "   A  " + state(firstStatus == null ? null : firstStatus.getInitSide())
          + "   |   B  " + state(firstStatus == null ? null : firstStatus.getReceiveSide()))
          .setFontSize(8).setFontColor(MUTED).setMarginBottom(2));
      document.add(new Paragraph(label(data, "最终记录（生命 / 能量）", "Last recorded state (HP / energy)")
          + "   A  " + state(lastStatus == null ? null : lastStatus.getInitSide())
          + "   |   B  " + state(lastStatus == null ? null : lastStatus.getReceiveSide()))
          .setFontSize(8).setFontColor(MUTED).setMarginBottom(14));
      addRounds(document, data, statuses);
    }
  }

  private Cell fighter(Data data, CharacterEntity character, ImageData portrait, String side,
      Integer before, Integer after) {
    Table card = new Table(UnitValue.createPercentArray(new float[] {26, 74})).useAllAvailableWidth();
    card.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).setPaddingRight(8)
        .setVerticalAlignment(VerticalAlignment.MIDDLE).add(new Image(portrait).scaleToFit(54, 64)));
    Cell details = new Cell().setBorder(Border.NO_BORDER).setPadding(0);
    details.add(new Paragraph(side + "  " + characterName(character, data.locale(), side)).setBold().setFontSize(11)
        .setMarginBottom(4));
    details.add(new Paragraph(label(data, "积分  ", "Score  ") + value(before) + label(data, " → ", " to ") + value(after)
        + (before == null || after == null ? "" : " (" + signed(after - before) + ")"))
        .setFontColor(BLUE).setFontSize(9).setMarginBottom(5));
    details.add(new Paragraph(label(data, "攻击 ", "ATK ") + value(character.getAttack()) + "   "
        + label(data, "防御 ", "DEF ") + value(character.getDefense()) + "   "
        + label(data, "速度 ", "SPD ") + value(character.getSpeed()))
        .setFontSize(7).setFontColor(MUTED).setMarginBottom(1));
    details.add(new Paragraph(label(data, "生命 ", "HP ") + value(character.getBlood()) + "   "
        + label(data, "能量 ", "EN ") + value(character.getEnergy()) + "   "
        + label(data, "智力 ", "INT ") + value(character.getIntelligence()))
        .setFontSize(7).setFontColor(MUTED).setMarginTop(0).setMarginBottom(0));
    card.addCell(details);
    return new Cell().setBorder(new SolidBorder(LINE, 0.5f)).setPadding(9).add(card);
  }

  private void addRounds(Document document, Data data, Map<Integer, BattleStatusDetailEntity> statuses) {
    document.add(new Paragraph(label(data, "逐回合记录", "Round log")).setFontSize(12).setBold()
        .setKeepWithNext(true).setMarginBottom(3));
    document.add(new Paragraph(label(data, "能耗按进攻 / 防守显示；生命和能量为回合结束时数值，缺失记录以 - 表示。",
        "Energy cost: attacker / defender. HP and energy are end-of-round values; - means unavailable."))
        .setFontSize(7).setFontColor(MUTED).setMarginBottom(7).setKeepWithNext(true));
    if (data.processes().isEmpty()) {
      document.add(new Paragraph(label(data, "暂无回合记录", "No round records available")).setFontColor(MUTED));
      return;
    }
    Table table = new Table(UnitValue.createPercentArray(new float[] {6, 12, 12, 12, 19, 19, 20}), true)
        .useAllAvailableWidth().setFontSize(8);
    for (String heading : List.of(label(data, "回合", "Round"), label(data, "行动", "Action"),
        label(data, "结果 / 伤害", "Hit / damage"), label(data, "能耗", "Energy cost"),
        label(data, "A 生命 / 能量", "A HP / energy"), label(data, "B 生命 / 能量", "B HP / energy"),
        label(data, "技能", "Skill"))) {
      table.addHeaderCell(cell(heading).setBackgroundColor(INK).setFontColor(WHITE)
          .setFontSize(7).setBold());
    }
    document.add(table);
    var processes = data.processes().stream().sorted(Comparator.comparing(
        BattleProcessDetailEntity::getBattleProcessRoundNumber, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    int row = 0;
    for (var process : processes) {
      var round = process.getBattleProcessRoundNumber();
      var after = round == null ? null : statuses.get(round + 1);
      String direction = data.starter().getId().equals(process.getAttackerId()) ? "A > B"
          : data.receiver().getId().equals(process.getAttackerId()) ? "B > A" : "-";
      String damage = Boolean.TRUE.equals(process.getAttackMissed()) ? label(data, "闪避", "Miss")
          : value(process.getDefenderBloodLoss());
      String skill = process.getAttackSkill() == null || process.getAttackSkill().isBlank() ? "-" : process.getAttackSkill();
      boolean expandedSkill = skill.codePointCount(0, skill.length()) > 80 || skill.contains("\n");
      DeviceRgb background = row++ % 2 == 0 ? WHITE : STRIPE;
      for (String text : List.of(value(round), direction, damage,
          value(process.getAttackerEnergyLoss()) + " / " + value(process.getDefenderEnergyLoss()),
          state(after == null ? null : after.getInitSide()), state(after == null ? null : after.getReceiveSide()),
          expandedSkill ? label(data, "详见下方", "See below") : skill)) {
        table.addCell(cell(text).setBackgroundColor(background));
      }
      if (expandedSkill) {
        table.addCell(cell(label(data, "技能  ", "Skill  ") + skill, 7).setBackgroundColor(background)
            .setFontSize(7.5f).setFontColor(MUTED));
      }
      if (row % 24 == 0) {
        table.flush();
      }
    }
    table.complete();
  }

  private Cell cell(String text) {
    return cell(text, 1);
  }

  private Cell cell(String text, int columns) {
    return new Cell(1, columns).setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(LINE, 0.3f))
        .setPaddingTop(5).setPaddingBottom(5).setPaddingLeft(4).setPaddingRight(4)
        .setVerticalAlignment(VerticalAlignment.MIDDLE)
        .add(new Paragraph(text).setMargin(0).setMultipliedLeading(1.2f));
  }

  private String state(CurrentCharacterEntity character) {
    return character == null ? "-" : value(character.getBlood()) + " / " + value(character.getCurrentEnergy());
  }

  private String winnerName(Data data) {
    UUID winnerId = data.execution().getWinnerId();
    if (data.starter().getId().equals(winnerId)) {
      return characterName(data.starter(), data.locale(), "A");
    }
    return data.receiver().getId().equals(winnerId) ? characterName(data.receiver(), data.locale(), "B") : null;
  }

  static String characterName(CharacterEntity character, String locale, String fallback) {
    if (character.getName() == null) {
      return fallback;
    }
    String primary = OnepieceLocale.ZH_CN.getValue().equals(locale) ? character.getName().getZhCN() : character.getName().getEnUS();
    String secondary = OnepieceLocale.ZH_CN.getValue().equals(locale) ? character.getName().getEnUS() : character.getName().getZhCN();
    return primary != null && !primary.isBlank() ? primary : secondary != null && !secondary.isBlank() ? secondary : fallback;
  }

  private String label(Data data, String chinese, String english) {
    return OnepieceLocale.ZH_CN.getValue().equals(data.locale()) ? chinese : english;
  }

  private String value(Integer number) {
    return number == null ? "-" : number.toString();
  }

  private boolean asciiOnly(Data data) {
    return !OnepieceLocale.ZH_CN.getValue().equals(data.locale())
        && ascii(characterName(data.starter(), data.locale(), "A"))
        && ascii(characterName(data.receiver(), data.locale(), "B"))
        && ascii(data.generatedBy())
        && data.processes().stream().allMatch(process -> ascii(process.getAttackSkill()));
  }

  private boolean ascii(String text) {
    return text == null || text.chars().allMatch(character -> character <= 127);
  }

  private String signed(int number) {
    return number > 0 ? "+" + number : String.valueOf(number);
  }

  private static class Footer implements IEventHandler {
    private final PdfFont font;
    private final String generatedBy;
    private final String timestamp = ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).format(TIMESTAMP);

    private Footer(PdfFont font, String generatedBy) {
      this.font = font;
      String author = generatedBy == null || generatedBy.isBlank() ? "Onepiece Battles" : generatedBy;
      while (font.getWidth(author, 7) > 220 && !author.isEmpty()) {
        author = author.substring(0, author.offsetByCodePoints(author.length(), -1));
      }
      this.generatedBy = author;
    }

    @Override
    public void handleEvent(com.itextpdf.kernel.events.Event event) {
      var pageEvent = (PdfDocumentEvent) event;
      var page = pageEvent.getPage();
      var size = page.getPageSize();
      var canvas = new PdfCanvas(page.newContentStreamAfter(), page.getResources(), pageEvent.getDocument());
      canvas.setStrokeColor(LINE).setLineWidth(0.4f).moveTo(36, 32).lineTo(size.getWidth() - 36, 32).stroke();
      try (var footer = new Canvas(canvas, size)) {
        footer.setFont(font).setFontSize(7).setFontColor(MUTED);
        footer.showTextAligned(generatedBy, 36, 20, TextAlignment.LEFT);
        footer.showTextAligned(String.valueOf(pageEvent.getDocument().getPageNumber(page)), size.getWidth() / 2, 20, TextAlignment.CENTER);
        footer.showTextAligned(timestamp, size.getWidth() - 36, 20, TextAlignment.RIGHT);
      }
    }
  }
}
