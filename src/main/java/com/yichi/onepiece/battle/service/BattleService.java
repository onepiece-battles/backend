package com.yichi.onepiece.battle.service;

import com.yichi.onepiece.battle.core.Battlefield;
import com.yichi.onepiece.battle.core.CharacterInBattle;
import com.yichi.onepiece.battle.model.BattleDao;
import com.yichi.onepiece.battle.model.BattleEntity;
import com.yichi.onepiece.battle.model.BattleEntityBuilder;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.fruits.service.FruitService;
import com.yichi.onepiece.model.*;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BattleService {
  Logger logger = LoggerFactory.getLogger(BattleService.class);
  private final BattleDao battleDao;
  private final CharacterService characterService;
  private final FruitService fruitService;

  public BattleService(
      BattleDao battleDao, CharacterService characterService, FruitService fruitService) {
    this.battleDao = battleDao;
    this.characterService = characterService;
    this.fruitService = fruitService;
  }

  public Battle addBattle(AddBattleRequest request) {
    BattleEntity battleEntity =
        BattleEntityBuilder.newBuilder()
            .setUserId(request.getUserId())
            // this service focus on
            .setBattleType(BattleType.SINGLE)
            .setCharacterIdInitiator(request.getInitiateCharacterId())
            .setCharacterIdReceiver(request.getReceiverCharacterId())
            .build();

    battleDao.insert(battleEntity);
    var insertedEntity =
        battleDao
            .findById(battleEntity.getId())
            .orElseThrow(ExceptionUtils.notFound("Battle entity not found"));
    return new Battle()
        .battleId(insertedEntity.getId())
        .userId(insertedEntity.getUserId())
        .type(insertedEntity.getBattleType())
        .initiateCharacterId(insertedEntity.getCharacterIdInitiator())
        .receiverCharacterId(insertedEntity.getCharacterIdReceiver());
  }

  public StartBattleResponse startBattle(UUID battleId) {
    var battle =
        battleDao
            .findById(battleId)
            .orElseThrow(ExceptionUtils.notFound("Battle with id %s not found", battleId));
    UUID battleInitId = battle.getCharacterIdInitiator();
    UUID battleRecId = battle.getCharacterIdReceiver();
    // prepare start character
    var startCharacter = characterService.getCharacterEntityById(battleInitId);
    var startFruits = startCharacter.getFruits();
    var startFruitEntities =
        fruitService.getFruitsByIds(
            startFruits.stream().map(FruitInCharacter::getFruitId).collect(Collectors.toList()));
    // prepare receive character
    var receiveCharacter = characterService.getCharacterEntityById(battleRecId);
    var receiveFruits = receiveCharacter.getFruits();
    var receiveFruitEntities =
        fruitService.getFruitsByIds(
            receiveFruits.stream().map(FruitInCharacter::getFruitId).collect(Collectors.toList()));
    // create characterInBattle object and battlefield object
    var characterInBattleInit = new CharacterInBattle(startCharacter, startFruitEntities);
    var characterInBattleReceive = new CharacterInBattle(receiveCharacter, receiveFruitEntities);
    var battlefield = new Battlefield(characterInBattleInit, characterInBattleReceive);

    while (battlefield.allAlive()) {
      battlefield.checkReverse();
      battlefield.processBattle();
      battlefield.attackReset();
    }
    logger.info("Battle finish");
    var winner = battlefield.getWinner();
    var loser = battlefield.getLoser();
    logger.info("Winner is {}", winner.getName());
    logger.info("Loser is {}", loser.getName());
    logger.info("------------------------------------------------");
    logger.info("winner information");
    logger.info("winner blood: {}, winner magic: {}", winner.getBlood(), winner.getMagic());
    logger.info("loser information");
    logger.info("loser blood: {}, loser magic: {}", loser.getBlood(), loser.getMagic());

    return new StartBattleResponse();
  }
}
