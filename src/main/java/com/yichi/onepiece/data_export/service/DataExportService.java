package com.yichi.onepiece.data_export.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.yichi.onepiece.alicloud.AliCloudObjectStorage;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.file.model.FileCategory;
import com.yichi.onepiece.file.model.FileObjectEntityBuilder;
import com.yichi.onepiece.file.service.FileObjectService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.user.UserHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class DataExportService {
  private final BattleService battleService;
  private final CharacterService characterService;
  private final FileObjectService fileObjectService;

  private final ActionRecordService actionRecordService;

  public DataExportService(
      BattleService battleService,
      CharacterService characterService,
      FileObjectService fileObjectService,
      ActionRecordService actionRecordService) {
    this.battleService = battleService;
    this.characterService = characterService;
    this.fileObjectService = fileObjectService;
    this.actionRecordService = actionRecordService;
  }

  /**
   * Export pdf data to AliYun oss and return the path.
   *
   * @param battleId id of battle that will be analyzed.
   * @return Path to the exported pdf.
   */
  public UUID createBattleResultPdf(UUID battleId) {
    var user = UserHelper.getCurrentUserDetail();
    var battleEntity = battleService.findBattleById(battleId);
    var statusList = battleService.listBattleStatusDetailByBattleId(battleId);
    var processList = battleService.listBattleProcessDetailByBattleId(battleId);
    var starterCharacter = characterService.findByCharacterId(battleEntity.getStarterId());
    var receiverCharacter = characterService.findByCharacterId(battleEntity.getReceiverId());
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      Rectangle rectangle = new Rectangle(PageSize.A4);
      rectangle.setBackgroundColor(BaseColor.GRAY);
      Document doc = new Document(rectangle);
      PdfWriter writer = PdfWriter.getInstance(doc, outputStream);
      doc.addTitle("battle");
      doc.addAuthor(user.getName());
      doc.addSubject("battle details");
      doc.addCreator("onepiece-battles");
      doc.setMargins(30, 30, 30, 30);
      doc.open();
      doc.add(new Paragraph("paragraph 1"));
      doc.add(new Paragraph("paragraph 2"));
      doc.add(new Paragraph("paragraph 3"));
      doc.close();
      writer.close();
      String uri =
          AliCloudObjectStorage.putFileStream(new ByteArrayInputStream(outputStream.toByteArray()));
      var actionRecordEntity = actionRecordService.create();
      var fileObjectEntity =
          FileObjectEntityBuilder.newBuilder()
              .setFileName(
                  getFileName(
                      user, starterCharacter.getName(), receiverCharacter.getName(), battleId))
              .setFileCategory(FileCategory.SINGLE_BATTLE_DETAIL)
              .setHttpMimeType("application/pdf")
              .setCreatorUserId(user.getId())
              .setCreationRecordId(actionRecordEntity.getId())
              .setOssUri(uri)
              .setResourceId(battleId)
              .build();
      fileObjectService.add(fileObjectEntity);
      return fileObjectEntity.getId();
    } catch (Exception e) {
      e.printStackTrace();
      // todo(yichi): handle exception.
      return null;
    }
  }

  private String getFileName(
      UserDetails user, I18nText starterCharacter, I18nText receiverCharacter, UUID battleId) {
    return String.format(
        "Created By %s, %s to %s, %s",
        user.getUsername(), starterCharacter, receiverCharacter, battleId);
  }
}
