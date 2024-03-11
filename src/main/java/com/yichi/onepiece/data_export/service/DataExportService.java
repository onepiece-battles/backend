package com.yichi.onepiece.data_export.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.file.model.FileCategory;
import com.yichi.onepiece.file.model.FileObjectEntityBuilder;
import com.yichi.onepiece.file.service.FileObjectService;
import com.yichi.onepiece.iam.service.RoleService;
import com.yichi.onepiece.lineage.model.ActionRecordEntityBuilder;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.BattleReportStage;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.UserRole;
import com.yichi.onepiece.user.UserHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

import com.yichi.onepiece.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
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
      logger.info("start creating files");
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Rectangle rectangle = new Rectangle(PageSize.A4);
      rectangle.setBackgroundColor(BaseColor.WHITE);
      Document doc = new Document(rectangle);
      PdfWriter writer = PdfWriter.getInstance(doc, outputStream);
      doc.addTitle("battle");
      doc.addAuthor(String.format("%s %s", userEmail, userNickname));
      doc.addSubject("battle details");
      doc.addCreator(String.format("%s %s", userEmail, userNickname));
      doc.setMargins(30, 30, 30, 30);
      doc.open();
      for (var status: statusList) {
        doc.add(new Paragraph(status.toString()));
      }
      for (var process: processList) {
        doc.add(new Paragraph(process.toString()));
      }
      doc.close();
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
            battleId, starterCharacter, receiverCharacter);
  }
}
