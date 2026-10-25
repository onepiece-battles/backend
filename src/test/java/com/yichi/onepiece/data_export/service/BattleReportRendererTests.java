package com.yichi.onepiece.data_export.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.yichi.onepiece.battle.model.BattleProcessDetailEntity;
import com.yichi.onepiece.battle.model.BattleProcessDetailEntityBuilder;
import com.yichi.onepiece.battle.model.BattleStatusDetailEntity;
import com.yichi.onepiece.battle.model.BattleStatusDetailEntityBuilder;
import com.yichi.onepiece.battle.model.CurrentCharacterEntityBuilder;
import com.yichi.onepiece.character.model.entity.CharacterEntityBuilder;
import com.yichi.onepiece.model.BattleExecution;
import com.yichi.onepiece.model.I18nText;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import org.testng.annotations.Test;

public class BattleReportRendererTests {
  private static final UUID STARTER = new UUID(0, 1);
  private static final UUID RECEIVER = new UUID(0, 2);

  @Test
  public void joinsPostRoundStateByRoundNumberAndKeepsRecordedZeroEnergyCost() throws Exception {
    var data = data(List.of(process(2, "ROUND-TWO"), process(1, "ROUND-ONE")),
        List.of(status(3, 33333), status(1, 99999), status(2, 11111)));
    String text = text(render(data));
    assertThat(text).contains("99999 / 47", "11111 / 47", "33333 / 47", "0 / 0");
    assertThat(text.substring(text.indexOf("Round log"))).doesNotContain("99999");
    assertThat(text.indexOf("ROUND-ONE")).isLessThan(text.indexOf("ROUND-TWO"));
  }

  @Test
  public void rendersMissingSnapshotsWithoutDroppingActionsOrInventingAWinner() throws Exception {
    var data = data(List.of(process(1, "FIRST-ACTION"), process(4, "LAST-ACTION")), List.of());
    data.execution().setWinnerId(null);
    assertThat(text(render(data))).contains("FIRST-ACTION", "LAST-ACTION", "Winner not recorded");
  }

  @Test
  public void rendersEmptyHistoryAsAReadableSinglePageReport() throws Exception {
    byte[] bytes = render(data(List.of(), List.of()));
    assertThat(text(bytes)).contains("No round records available", "0 rounds");
    try (var pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
      assertThat(pdf.getNumberOfPages()).isEqualTo(1);
    }
  }

  @Test
  public void englishReportRetainsNonAsciiNamesAndSkillsThroughUnicodeFontFallback() throws Exception {
    var source = data(List.of(process(1, "橡胶火拳")), List.of(status(2, 11111)));
    var starter = CharacterEntityBuilder.from(source.starter())
        .setName(new I18nText().enUS("蒙奇 Luffy")).build();
    var unicode = new BattleReportRenderer.Data(source.battleId(), source.execution(), starter, source.receiver(),
        source.statuses(), source.processes(), "en_US", "测试用户@example.test");
    assertThat(text(render(unicode))).contains("蒙奇", "橡胶", "Luffy");
  }

  @Test
  public void longNamesAndSkillsWrapAcrossPagesWithoutLosingTheLastText() throws Exception {
    var source = data(List.of(process(1, "Long technique description ".repeat(250) + "END-OF-SKILL")), List.of(status(2, 11111)));
    var starter = CharacterEntityBuilder.from(source.starter())
        .setName(new I18nText().enUS("Captain ".repeat(18) + "NAME-END")).build();
    var lengthy = new BattleReportRenderer.Data(source.battleId(), source.execution(), starter, source.receiver(),
        source.statuses(), source.processes(), "en_US", "very-long-email-address-".repeat(8) + "@example.test");
    byte[] bytes = render(lengthy);
    assertThat(text(bytes)).contains("NAME-END", "END-OF-SKILL");
    if (System.getenv("ONEPIECE_PDF_PREVIEW_DIR") != null) {
      Files.createDirectories(Path.of("tmp/pdfs"));
      Files.write(Path.of("tmp/pdfs/edge-long.pdf"), bytes);
    }
  }

  @Test
  public void concurrentReportsKeepFontAndPdfObjectsIsolated() throws Exception {
    var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var first = workers.submit(() -> text(render(data(List.of(process(1, "蒙奇 FIRST-ONLY")), List.of()))));
      var second = workers.submit(() -> text(render(data(List.of(process(1, "罗布 SECOND-ONLY")), List.of()))));
      assertThat(first.get(5, java.util.concurrent.TimeUnit.SECONDS)).contains("FIRST-ONLY").doesNotContain("SECOND-ONLY");
      assertThat(second.get(5, java.util.concurrent.TimeUnit.SECONDS)).contains("SECOND-ONLY").doesNotContain("FIRST-ONLY");
    } finally {
      workers.shutdownNow();
    }
  }

  private BattleReportRenderer.Data data(List<BattleProcessDetailEntity> processes, List<BattleStatusDetailEntity> statuses) {
    var starter = CharacterEntityBuilder.newBuilder().setId(STARTER)
        .setName(new I18nText().enUS("Luffy").zhCN("路飞")).setBlood(5000).setEnergy(100).build();
    var receiver = CharacterEntityBuilder.from(starter).setId(RECEIVER).setName(new I18nText().enUS("Lucci")).build();
    return new BattleReportRenderer.Data(UUID.randomUUID(), new BattleExecution().winnerId(STARTER)
        .starterScoreBefore(5000).starterScoreAfter(5100).receiverScoreBefore(5000).receiverScoreAfter(4900),
        starter, receiver, statuses, processes, "en_US", "captain@example.test");
  }

  private BattleProcessDetailEntity process(int round, String skill) {
    return BattleProcessDetailEntityBuilder.newBuilder().setBattleProcessRoundNumber(round).setAttackerId(STARTER)
        .setAttackerEnergyLoss(0).setDefenderEnergyLoss(0).setDefenderBloodLoss(123)
        .setAttackMissed(false).setAttackSkill(skill).build();
  }

  private BattleStatusDetailEntity status(int round, int blood) {
    var state = CurrentCharacterEntityBuilder.newBuilder().setBlood(blood).setCurrentEnergy(47).build();
    return BattleStatusDetailEntityBuilder.newBuilder().setBattleStatusRoundNumber(round)
        .setInitSide(state).setReceiveSide(state).build();
  }

  private byte[] render(BattleReportRenderer.Data data) throws Exception {
    var output = new ByteArrayOutputStream();
    var image = ImageDataFactory.create(BattleReportGenerationTests.Images.REPORT);
    new BattleReportRenderer().render(output, data, image, image);
    return output.toByteArray();
  }

  private String text(byte[] bytes) throws Exception {
    var text = new StringBuilder();
    try (var pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
      for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
        text.append(PdfTextExtractor.getTextFromPage(pdf.getPage(page)));
      }
    }
    return Normalizer.normalize(text.toString(), Normalizer.Form.NFKC);
  }
}
