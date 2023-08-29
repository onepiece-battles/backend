package com.yichi.onepiece.battle.service;

import com.yichi.onepiece.battle.core.Battlefield;
import com.yichi.onepiece.battle.core.CharacterInBattle;
import com.yichi.onepiece.battle.model.*;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import java.util.List;
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
  private final ActionRecordService actionRecordService;
  private final BattleProcessDetailDao battleProcessDetailDao;
  private final BattleStatusDetailDao battleStatusDetailDao;

  public BattleService(
      BattleDao battleDao,
      CharacterService characterService,
      FruitService fruitService,
      ActionRecordService actionRecordService,
      BattleProcessDetailDao battleProcessDetailDao,
      BattleStatusDetailDao battleStatusDetailDao) {
    this.battleDao = battleDao;
    this.characterService = characterService;
    this.fruitService = fruitService;
    this.actionRecordService = actionRecordService;
    this.battleProcessDetailDao = battleProcessDetailDao;
    this.battleStatusDetailDao = battleStatusDetailDao;
  }

  public Battle addBattle(AddBattleRequest request) {
    var actionRecord = actionRecordService.create();
    BattleEntity battleEntity =
        BattleEntityBuilder.newBuilder()
            .setUserId(request.getUserId())
            // this service focus on
            .setType(BattleType.SINGLE)
            .setStatus(BattleStatus.CREATED)
            .setStarterId(request.getStarterId())
            .setReceiverId(request.getReceiverId())
            .setCreationRecordId(actionRecord.getId())
            .setModificationRecordId(actionRecord.getId())
            .build();

    battleDao.insert(battleEntity);
    var insertedEntity =
        battleDao
            .findById(battleEntity.getId())
            .orElseThrow(ExceptionUtils.notFound("Battle entity not found"));
    return new Battle()
        .id(insertedEntity.getId())
        .userId(insertedEntity.getUserId())
        .type(insertedEntity.getType())
        .starterId(insertedEntity.getStarterId())
        .receiverId(insertedEntity.getReceiverId());
  }

  public ListBattleResponse list(ListBattleRequest request) {
    var totalSize = battleDao.count();
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return new ListBattleResponse().battles(List.of()).totalSize(totalSize);
    }
    var battleList =
        battleDao
            .list(request.getFruitFilter(), request.getSortKeys(), offset, request.getPageSize())
            .stream()
            .map(BattleConverter.INSTANCE::toBattle)
            .collect(Collectors.toList());
    return new ListBattleResponse().battles(battleList).totalSize(totalSize);
  }

  public StartBattleResponse startBattle(UUID battleId) {
    var battle =
        battleDao
            .findById(battleId)
            .orElseThrow(ExceptionUtils.notFound("Battle with id %s not found", battleId));
    // update status
    var battleStarted = battle.toBuilder().setStatus(BattleStatus.STARTED).build();
    battleDao.update(battleStarted);
    UUID starterId = battle.getStarterId();
    UUID receiverId = battle.getReceiverId();
    // prepare start character
    var startCharacter = characterService.getCharacterEntityById(starterId);
    var startFruits = startCharacter.getFruits();
    var startFruitEntities =
        fruitService.getFruitsByIds(
            startFruits.stream().map(FruitInCharacter::getFruitId).collect(Collectors.toList()));
    // prepare receive character
    var receiveCharacter = characterService.getCharacterEntityById(receiverId);
    var receiveFruits = receiveCharacter.getFruits();
    var receiveFruitEntities =
        fruitService.getFruitsByIds(
            receiveFruits.stream().map(FruitInCharacter::getFruitId).collect(Collectors.toList()));
    // create characterInBattle object and battlefield object
    var characterInBattleInit = new CharacterInBattle(startCharacter, startFruitEntities);
    var characterInBattleReceive = new CharacterInBattle(receiveCharacter, receiveFruitEntities);
    var battlefield = new Battlefield(characterInBattleInit, characterInBattleReceive, battleId);

    while (battlefield.allAlive()) {
      battlefield.checkReverse();
      battlefield.processBattle();
      battlefield.attackReset();
    }
    battlefield.completeFinalStatus();
    battleStatusDetailDao.batchInsert(battlefield.getBattleStatusDetailEntities());
    battleProcessDetailDao.batchInsert(battlefield.getBattleProcessDetailEntities());
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

  public List<BattleStatusDetailEntity> listBattleStatusDetailByBattleId(UUID battleId) {
    return battleStatusDetailDao.listByBattleId(battleId);
  }

  public List<BattleProcessDetailEntity> listBattleProcessDetailByBattleId(UUID battleId) {
    return battleProcessDetailDao.listByBattleId(battleId);
  }

  public BattleEntity findBattleById(UUID battleId) {
    return battleDao.findById(battleId).orElseThrow();
  }
}
