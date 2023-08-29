package com.yichi.onepiece.pdf_export.service;

import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.character.service.CharacterService;
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

  public void createBattleResultPdf(UUID battleId) {
    var battleEntity = battleService.findBattleById(battleId);
    var statusList = battleService.listBattleStatusDetailByBattleId(battleId);
    var processList = battleService.listBattleProcessDetailByBattleId(battleId);
    var starterCharacter = characterService.findByCharacterId(battleEntity.getStarterId());
    var receiverCharacter = characterService.findByCharacterId(battleEntity.getReceiverId());
  }
}
