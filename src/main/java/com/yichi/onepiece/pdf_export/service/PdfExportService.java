package com.yichi.onepiece.pdf_export.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.yichi.onepiece.alicloud.AliCloudOssHelper;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.user.UserHelper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PdfExportService {
  private final BattleService battleService;
  private final CharacterService characterService;


  public PdfExportService(BattleService battleService, CharacterService characterService) {
    this.battleService = battleService;
    this.characterService = characterService;
  }

  /**
   * Export pdf data to AliYun oss and return the path.
   *
   * @param battleId id of battle that will be analyzed.
   * @return Path to the exported pdf.
   */
  public String createBattleResultPdf(UUID battleId) throws FileNotFoundException {
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
      PdfWriter writer =  PdfWriter.getInstance(doc, outputStream);
      doc.addTitle("battle");
      doc.addAuthor(user.getName());
      doc.addSubject("battle details");
      doc.addCreator("onepiece-battles");
      doc.setMargins(30,30,30,30);
      doc.open();
      doc.add(new Paragraph("paragraph 1"));
      doc.add(new Paragraph("paragraph 2"));
      doc.add(new Paragraph("paragraph 3"));
      doc.close();
      writer.close();
      AliCloudOssHelper.put("battle", new ByteArrayInputStream(outputStream.toByteArray()));
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "path";
  }
}
