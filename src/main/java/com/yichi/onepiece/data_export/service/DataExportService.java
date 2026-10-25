package com.yichi.onepiece.data_export.service;

import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.file.model.FileCategory;
import com.yichi.onepiece.file.model.FileObjectEntityBuilder;
import com.yichi.onepiece.file.service.FileObjectService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.BattleReportStage;
import com.yichi.onepiece.model.UserRole;
import com.yichi.onepiece.user.service.UserService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DataExportService {
  private static final Logger logger = LoggerFactory.getLogger(DataExportService.class);
  private final BattleService battleService;
  private final CharacterService characterService;
  private final FileObjectService fileObjectService;
  private final ActionRecordService actionRecordService;
  private final UserService userService;

  public DataExportService(BattleService battleService, CharacterService characterService,
      FileObjectService fileObjectService, ActionRecordService actionRecordService, UserService userService) {
    this.battleService = battleService;
    this.characterService = characterService;
    this.fileObjectService = fileObjectService;
    this.actionRecordService = actionRecordService;
    this.userService = userService;
  }

  public UUID generateBattleResultPdf(UUID battleId, String userEmail, String locale, UUID userId, String roleName) {
    Path reportFile = null;
    long started = System.nanoTime();
    try {
      var battle = battleService.findBattleById(battleId);
      var execution = battleService.getBattleExecution(battleId);
      var statuses = battleService.listBattleStatusDetailByBattleId(battleId);
      var processes = battleService.listBattleProcessDetailByBattleId(battleId);
      var starter = characterService.findByCharacterId(battle.getStarterId());
      var receiver = characterService.findByCharacterId(battle.getReceiverId());
      var images = new ReportImageLoader();
      var starterImage = images.load(starter, BattleReportRenderer.characterName(starter, locale, "A"));
      var receiverImage = images.load(receiver, BattleReportRenderer.characterName(receiver, locale, "B"));
      reportFile = Files.createTempFile("battle-report-", ".pdf");
      try (var output = Files.newOutputStream(reportFile)) {
        new BattleReportRenderer().render(output,
            new BattleReportRenderer.Data(battleId, execution, starter, receiver, statuses, processes, locale, userEmail),
            starterImage, receiverImage);
      }
      long size = Files.size(reportFile);
      String uri;
      try (var input = Files.newInputStream(reportFile)) {
        uri = AliCloudObjectStorage.putFileStream(input, size);
      }
      var user = userService.getUserEntityById(userId);
      var record = actionRecordService.createWithUserAndRole(user, UserRole.fromValue(roleName));
      var file = FileObjectEntityBuilder.newBuilder().setId(UUID.randomUUID())
          .setFileName(String.format("%s_%s_%s", BattleReportRenderer.characterName(starter, "en_US", "A"),
              BattleReportRenderer.characterName(receiver, "en_US", "B"), battleId))
          .setFileCategory(FileCategory.SINGLE_BATTLE_DETAIL).setHttpMimeType("application/pdf")
          .setCreatorId(userId).setCreationRecordId(record.getId()).setOssUri(uri).setResourceId(battleId).build();
      fileObjectService.add(file);
      battleService.updateFileId(battleId, file.getId());
      battleService.updateBattleReportStage(battleId, BattleReportStage.FINISHED);
      logger.info("Generated battle report {}: {} bytes, {} ms", battleId, size, (System.nanoTime() - started) / 1_000_000);
      return file.getId();
    } catch (Exception exception) {
      logger.error("Failed to generate battle report for battle {}", battleId, exception);
      try {
        battleService.updateBattleReportStage(battleId, BattleReportStage.WAITING);
      } catch (Exception updateException) {
        logger.error("Failed to reset battle report stage for battle {}", battleId, updateException);
      }
      return null;
    } finally {
      if (reportFile != null) {
        try {
          Files.deleteIfExists(reportFile);
        } catch (Exception cleanupException) {
          logger.warn("Failed to delete temporary battle report {}", reportFile, cleanupException);
        }
      }
    }
  }
}
