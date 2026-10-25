package com.yichi.onepiece.data_export.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.battle.model.BattleEntityBuilder;
import com.yichi.onepiece.battle.model.BattleProcessDetailEntity;
import com.yichi.onepiece.battle.model.BattleProcessDetailEntityBuilder;
import com.yichi.onepiece.battle.model.BattleStatusDetailEntity;
import com.yichi.onepiece.battle.model.BattleStatusDetailEntityBuilder;
import com.yichi.onepiece.battle.model.CurrentCharacterEntityBuilder;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.character.model.entity.CharacterEntityBuilder;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.file.service.FileObjectService;
import com.yichi.onepiece.exception.FileStorageException;
import com.yichi.onepiece.lineage.model.ActionRecordEntityBuilder;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.BattleExecution;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.UserRole;
import com.yichi.onepiece.model.BattleReportStage;
import com.yichi.onepiece.user.model.UserEntityBuilder;
import com.yichi.onepiece.user.service.UserService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class BattleReportGenerationTests {
  @DataProvider
  public Object[][] reports() {
    return new Object[][] {{"zh_CN", 12, false}, {"en_US", 12, false}, {"zh_CN", 120, false}, {"en_US", 0, true}};
  }

  @Test(dataProvider = "reports")
  public void generatesReadableReportAndOptionalPreview(String locale, int rounds, boolean uploadFailure) throws Exception {
    var battleService = mock(BattleService.class);
    var characters = mock(CharacterService.class);
    var users = mock(UserService.class);
    var records = mock(ActionRecordService.class);
    var files = mock(FileObjectService.class);
    var battleId = UUID.fromString("12345678-1234-4321-9876-123456789abc");
    var starterId = UUID.randomUUID();
    var receiverId = UUID.randomUUID();
    var userId = UUID.randomUUID();
    var starter = CharacterEntityBuilder.newBuilder().setId(starterId)
        .setName(new I18nText().zhCN("蒙奇·D·路飞").enUS("Monkey D. Luffy"))
        .setImageUri("starter-image").setAttack(230).setDefense(180).setSpeed(220)
        .setIntelligence(190).setBlood(5000).setEnergy(100).build();
    var receiver = CharacterEntityBuilder.from(starter).setId(receiverId)
        .setName(new I18nText().zhCN("罗布·路奇").enUS("Rob Lucci")).setImageUri("receiver-image").build();
    when(battleService.findBattleById(battleId)).thenReturn(BattleEntityBuilder.newBuilder().setId(battleId)
        .setStarterId(starterId).setReceiverId(receiverId).build());
    when(battleService.getBattleExecution(battleId)).thenReturn(new BattleExecution().winnerId(starterId)
        .starterScoreBefore(8000).starterScoreAfter(8250).receiverScoreBefore(7900).receiverScoreAfter(7650)
        .winnerScoreChange(250).loserScoreChange(-250));
    when(characters.findByCharacterId(starterId)).thenReturn(starter);
    when(characters.findByCharacterId(receiverId)).thenReturn(receiver);
    when(users.getUserEntityById(userId)).thenReturn(UserEntityBuilder.newBuilder().setId(userId).build());
    when(records.createWithUserAndRole(any(), eq(UserRole.NORMAL)))
        .thenReturn(ActionRecordEntityBuilder.newBuilder().setId(1L).build());
    List<BattleStatusDetailEntity> statuses = new ArrayList<>();
    List<BattleProcessDetailEntity> processes = new ArrayList<>();
    for (int round = 1; round <= rounds + 1; round++) {
      statuses.add(BattleStatusDetailEntityBuilder.newBuilder().setBattleStatusRoundNumber(round)
          .setInitSide(CurrentCharacterEntityBuilder.newBuilder().setCharacterId(starterId)
              .setBlood(5000 - (round - 1) * 12).setCurrentEnergy(Math.max(0, 100 - round / 2)).build())
          .setReceiveSide(CurrentCharacterEntityBuilder.newBuilder().setCharacterId(receiverId)
              .setBlood(round == rounds + 1 ? 0 : 5000 - (round - 1) * 36)
              .setCurrentEnergy(Math.max(0, 100 - round / 2)).build()).build());
      if (round <= rounds) {
        processes.add(BattleProcessDetailEntityBuilder.newBuilder().setBattleProcessRoundNumber(round)
            .setAttackerId(round % 2 == 1 ? starterId : receiverId).setAttackMissed(round % 3 == 0)
            .setAttackerEnergyLoss(round == 2 ? 0 : 2).setDefenderEnergyLoss(0)
            .setDefenderBloodLoss(round % 3 == 0 ? 0 : 36)
            .setAttackSkill(locale.equals("zh_CN") ? "橡胶·火拳铳" : "Gum-Gum Red Hawk").build());
      }
    }
    when(battleService.listBattleStatusDetailByBattleId(battleId)).thenReturn(statuses);
    when(battleService.listBattleProcessDetailByBattleId(battleId)).thenReturn(processes);
    byte[] originalImage = Images.ORIGINAL;
    byte[] smallImage = Images.SMALL;
    byte[] reportImage = Images.REPORT;
    var uploaded = new AtomicReference<byte[]>();
    var uploadStream = new AtomicReference<InputStream>();
    var before = temporaryReports();
    String baselineClasses = System.getenv("ONEPIECE_PDF_BASELINE_CLASSES");
    boolean paired = baselineClasses != null && rounds >= 120 && !uploadFailure;
    LegacyReport legacy = paired ? new LegacyReport(baselineClasses, battleService, characters, files, records, users) : null;
    int runs = paired ? 7 : rounds >= 120 ? 6 : 1;
    try (var storage = mockStatic(AliCloudObjectStorage.class); legacy) {
      storage.when(() -> AliCloudObjectStorage.getReportImageStream(anyString()))
          .thenAnswer(invocation -> new ByteArrayInputStream(reportImage));
      storage.when(() -> AliCloudObjectStorage.getImageStream(anyString(), anyBoolean())).thenAnswer(invocation -> {
        boolean original = invocation.getArgument(1);
        return new ByteArrayInputStream(original ? originalImage : smallImage);
      });
      storage.when(() -> AliCloudObjectStorage.putFileStream(any(InputStream.class))).thenAnswer(invocation -> {
        uploaded.set(((InputStream) invocation.getArgument(0)).readAllBytes());
        return "test://report";
      });
      storage.when(() -> AliCloudObjectStorage.putFileStream(any(InputStream.class), anyLong())).thenAnswer(invocation -> {
        InputStream input = invocation.getArgument(0);
        uploadStream.set(input);
        uploaded.set(input.readAllBytes());
        long expectedLength = invocation.getArgument(1);
        assertThat((long) uploaded.get().length).isEqualTo(expectedLength);
        if (uploadFailure) {
          throw new FileStorageException("Simulated upload failure");
        }
        return "test://report";
      });
      List<Long> measurements = new ArrayList<>();
      List<Long> baselineMeasurements = new ArrayList<>();
      for (int iteration = 0; iteration < runs; iteration++) {
        if (paired && iteration % 2 == 0) {
          baselineMeasurements.add(timeLegacy(legacy, battleId, locale, userId, uploaded));
        }
        long started = System.nanoTime();
        var fileId = new DataExportService(battleService, characters, files, records, users)
            .generateBattleResultPdf(battleId, "captain@example.test", locale, userId, "NORMAL");
        assertThat(fileId == null).isEqualTo(uploadFailure);
        measurements.add((System.nanoTime() - started) / 1_000_000);
        byte[] currentPdf = uploaded.get();
        if (paired && iteration % 2 != 0) {
          baselineMeasurements.add(timeLegacy(legacy, battleId, locale, userId, uploaded));
        }
        uploaded.set(currentPdf);
      }
      long milliseconds = median(measurements, paired ? 2 : rounds >= 120 ? 1 : 0);
      if (paired) {
        System.out.printf("PDF_PAIRED_MEDIAN baselineMs=%d optimizedMs=%d%n", median(baselineMeasurements, 2), milliseconds);
      }
      assertThat(temporaryReports()).isEqualTo(before);
      assertThatThrownBy(() -> uploadStream.get().read()).isInstanceOf(IOException.class);
      if (uploadFailure) {
        verifyNoInteractions(files, records);
        verify(battleService).updateBattleReportStage(battleId, BattleReportStage.WAITING);
        return;
      }
      storage.verify(() -> AliCloudObjectStorage.getReportImageStream("starter-image"), times(runs));
      storage.verify(() -> AliCloudObjectStorage.getReportImageStream("receiver-image"), times(runs));
      storage.verify(() -> AliCloudObjectStorage.getImageStream(anyString(), anyBoolean()), times(paired ? 4 * runs : 0));
      verify(battleService, times(paired ? 2 * runs : runs)).updateBattleReportStage(battleId, BattleReportStage.FINISHED);
      assertThat(uploaded.get()).isNotNull();
      try (var pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(uploaded.get())))) {
        var text = new StringBuilder();
        for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
          text.append(PdfTextExtractor.getTextFromPage(pdf.getPage(page)));
        }
        String normalized = Normalizer.normalize(text.toString(), Normalizer.Form.NFKC);
        assertThat(normalized).contains(locale.equals("zh_CN") ? "战斗报告" : "Battle Report");
        assertThat(normalized).contains(locale.equals("zh_CN") ? "蒙奇" : "Luffy");
        assertThat(pdf.getNumberOfPages()).isLessThanOrEqualTo(rounds >= 120 ? 5 : 1);
        assertThat(uploaded.get().length).isLessThan(locale.equals("en_US") ? 50_000 : 350_000);
        var images = new HashSet<Integer>();
        var fonts = new HashSet<Integer>();
        for (int page = 1; page <= pdf.getNumberOfPages(); page++) {
          var resources = pdf.getPage(page).getResources();
          var objects = resources.getResource(PdfName.XObject);
          if (objects != null) {
            for (var name : objects.keySet()) {
              var object = objects.getAsStream(name);
              if (PdfName.Image.equals(object.getAsName(PdfName.Subtype))) {
                images.add(object.getIndirectReference().getObjNumber());
              }
            }
          }
          var pageFonts = resources.getResource(PdfName.Font);
          for (var name : pageFonts.keySet()) {
            fonts.add(pageFonts.getAsDictionary(name).getIndirectReference().getObjNumber());
          }
        }
        assertThat(images).hasSize(2);
        assertThat(fonts).hasSize(1);
        System.out.printf("PDF_METRICS locale=%s rounds=%d pages=%d bytes=%d generationMs=%d%n",
            locale, rounds, pdf.getNumberOfPages(), uploaded.get().length, milliseconds);
      }
      String previewDir = System.getenv("ONEPIECE_PDF_PREVIEW_DIR");
      if (previewDir != null) {
        Files.createDirectories(Path.of(previewDir));
        Files.write(Path.of(previewDir, locale + "-" + rounds + ".pdf"), uploaded.get());
      }
    }
  }

  private java.util.Set<Path> temporaryReports() throws IOException {
    try (var paths = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
      return paths.filter(path -> path.getFileName().toString().startsWith("battle-report-")).collect(java.util.stream.Collectors.toSet());
    }
  }

  private long median(List<Long> samples, int warmups) {
    return samples.stream().skip(warmups).sorted().toList().get((samples.size() - warmups) / 2);
  }

  private long timeLegacy(LegacyReport legacy, UUID battleId, String locale, UUID userId,
      AtomicReference<byte[]> uploaded) throws Exception {
    long started = System.nanoTime();
    legacy.generate(battleId, "captain@example.test", locale, userId, "NORMAL");
    long milliseconds = (System.nanoTime() - started) / 1_000_000;
    assertThat(uploaded.get().length).isGreaterThan(1_000_000);
    return milliseconds;
  }

  private static class LegacyReport implements AutoCloseable {
    private final java.net.URLClassLoader loader;
    private final Object service;
    private final java.lang.reflect.Method generate;

    private LegacyReport(String classes, BattleService battles, CharacterService characters, FileObjectService files,
        ActionRecordService records, UserService users) throws Exception {
      String serviceClass = DataExportService.class.getName();
      loader = new java.net.URLClassLoader(new java.net.URL[] {Path.of(classes).toUri().toURL()},
          DataExportService.class.getClassLoader()) {
        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
          if (!name.equals(serviceClass) && !name.startsWith(serviceClass + "$")) {
            return super.loadClass(name, resolve);
          }
          synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
              loaded = findClass(name);
            }
            if (resolve) {
              resolveClass(loaded);
            }
            return loaded;
          }
        }
      };
      Class<?> type = loader.loadClass(serviceClass);
      service = type.getConstructor(BattleService.class, CharacterService.class, FileObjectService.class,
          ActionRecordService.class, UserService.class).newInstance(battles, characters, files, records, users);
      generate = type.getMethod("generateBattleResultPdf", UUID.class, String.class, String.class, UUID.class, String.class);
    }

    private void generate(UUID battleId, String email, String locale, UUID userId, String role) throws Exception {
      generate.invoke(service, battleId, email, locale, userId, role);
    }

    @Override
    public void close() throws IOException {
      loader.close();
    }
  }

  static class Images {
    static final byte[] ORIGINAL = image(1200, false);
    static final byte[] SMALL = image(100, false);
    static final byte[] REPORT = image(256, true);

    static byte[] image(int size, boolean jpeg) {
      try {
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int vertical = 0; vertical < size; vertical++) {
          for (int horizontal = 0; horizontal < size; horizontal++) {
            int texture = (horizontal * 13 + vertical * 7) % 19;
            image.setRGB(horizontal, vertical, new Color(35 + texture, 70 + vertical * 90 / size,
                110 + horizontal * 90 / size).getRGB());
          }
        }
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(240, 183, 75));
        graphics.fillOval(size / 3, size / 6, size / 3, size / 3);
        graphics.fillRoundRect(size / 4, size / 2, size / 2, size / 3, size / 8, size / 8);
        graphics.dispose();
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, jpeg ? "jpg" : "png", output);
        return output.toByteArray();
      } catch (Exception exception) {
        throw new IllegalStateException(exception);
      }
    }
  }
}
