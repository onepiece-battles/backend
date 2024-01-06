package com.yichi.onepiece.battle.service;

import com.yichi.onepiece.battle.core.Battlefield;
import com.yichi.onepiece.battle.core.CharacterInBattle;
import com.yichi.onepiece.battle.model.*;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.model.BattleFullView;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BattleService {
  private static final ResourceType RESOURCE_TYPE = ResourceType.CHARACTER;
  Logger logger = LoggerFactory.getLogger(BattleService.class);
  private final BattleDao battleDao;
  private final BattleExecutionDao battleExecutionDao;
  private final CharacterService characterService;
  private final FruitService fruitService;
  private final ActionRecordService actionRecordService;
  private final BattleProcessDetailDao battleProcessDetailDao;
  private final BattleStatusDetailDao battleStatusDetailDao;
  private final UserService userService;

  public BattleService(
      BattleDao battleDao,
      BattleExecutionDao battleExecutionDao,
      CharacterService characterService,
      FruitService fruitService,
      ActionRecordService actionRecordService,
      BattleProcessDetailDao battleProcessDetailDao,
      BattleStatusDetailDao battleStatusDetailDao,
      UserService userService) {
    this.battleDao = battleDao;
    this.battleExecutionDao = battleExecutionDao;
    this.characterService = characterService;
    this.fruitService = fruitService;
    this.actionRecordService = actionRecordService;
    this.battleProcessDetailDao = battleProcessDetailDao;
    this.battleStatusDetailDao = battleStatusDetailDao;
    this.userService = userService;
  }

  public Battle createBattle(AddBattleRequest request) {
    var actionRecord = actionRecordService.create();
    BattleEntity battleEntity =
        BattleEntityBuilder.newBuilder()
            .setUserId(request.getUserId())
            // this service focus on
            .setType(BattleType.SINGLE)
            .setStarterId(request.getStarterId())
            .setReceiverId(request.getReceiverId())
            .setCreationRecordId(actionRecord.getId())
            .setModificationRecordId(actionRecord.getId())
            .build();
    battleDao.insert(battleEntity);
    BattleExecutionEntity battleExecutionEntity =
        BattleExecutionEntityBuilder.newBuilder()
            .setBattleId(battleEntity.getId())
            .setBattleStatus(BattleStatus.CREATED)
            .setBattleReportStage(BattleReportStage.WAITING)
            .creationTimestamp(actionRecord.getActionTimestamp())
            .modificationTimestamp(actionRecord.getActionTimestamp())
            .build();
    battleExecutionDao.insert(battleExecutionEntity);
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
    var userId = UserHelper.getCurrentUserId();
    var resourceIds = userService.getResourceIdsByUserIdResourceType(userId, RESOURCE_TYPE);
    var totalSize = battleDao.searchCount(userId, request.getBattleFilter(), resourceIds);
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return new ListBattleResponse().battles(List.of()).totalSize(totalSize);
    }
    var battleList =
        battleDao
            .list(
                request.getBattleFilter(),
                request.getSortKeys(),
                userId,
                resourceIds,
                offset,
                request.getPageSize())
            .stream()
            .map(BattleConverter.INSTANCE::toBattleListView)
            .toList();
    return new ListBattleResponse().battles(battleList).totalSize(totalSize);
  }

  public BattleFullView getBattleFullView(GetBattleFullViewRequest request) {
    UUID battleId = request.getBattleId();
    var battleFullViewEntity = battleDao.getBattleFullView(battleId).orElseThrow();
    return BattleConverter.INSTANCE.toBattleFullView(battleFullViewEntity);
  }

  public BattleExecutionEntity getBattleExecutionStatusByBattleId(UUID battleId) {
    return battleExecutionDao.findByBattleId(battleId).orElseThrow();
  }

  public void updateBattleExecutionEntity(BattleExecutionEntity battleExecutionEntity) {
    battleExecutionDao.update(battleExecutionEntity);
  }

  public BattleExecution getBattleLite(GetBattleExecutionRequest request) {
    var battleExecutionEntity =
        battleExecutionDao.findByBattleId(request.getBattleId()).orElseThrow();
    return BattleConverter.INSTANCE.toBattleExecution(battleExecutionEntity);
  }

  public ListBattleByIdsResponse batchFindById(ListBattleByIdsRequest request) {
    if (request.getBattleIds().isEmpty()) {
      return new ListBattleByIdsResponse().battles(List.of());
    }
    var battleEntities = battleDao.batchFindById(request.getBattleIds());
    var battles = battleEntities.stream().map(BattleConverter.INSTANCE::toBattle).toList();
    return new ListBattleByIdsResponse().battles(battles);
  }

  public void checkBattleStatus(BattleStatus status) {
    if (BattleStatus.CREATED.equals(status)) {
      return;
    }
    throw new RuntimeException("battle can not go to next process");
  }

  public void startBattle(UUID battleId) {
    var battle =
        battleDao
            .findById(battleId)
            .orElseThrow(ExceptionUtils.notFound("Battle with id %s not found", battleId));
    UUID starterId = battle.getStarterId();
    UUID receiverId = battle.getReceiverId();
    // prepare start character
    var startCharacter = characterService.getCharacterEntityById(starterId);
    var startFruits = fruitService.listByCharacterId(startCharacter.getId());
    // prepare receive character
    var receiveCharacter = characterService.getCharacterEntityById(receiverId);
    var receiveFruits = fruitService.listByCharacterId(receiveCharacter.getId());
    // create characterInBattle object and battlefield object
    var characterInBattleInit = new CharacterInBattle(startCharacter, startFruits);
    var characterInBattleReceive = new CharacterInBattle(receiveCharacter, receiveFruits);
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
    logger.info("winner blood: {}", winner.getBlood());
    logger.info("loser information");
    logger.info("loser blood: {}", loser.getBlood());
    new StartBattleResponse().battleId(battleId).result(new BattleResult());
  }

  // update pdf file id
  public void updateFileId(UUID battleId, UUID fileId) {
    var battleExecutionEntity = battleExecutionDao.findById(battleId).orElseThrow();
    battleExecutionEntity = battleExecutionEntity.toBuilder().setReportFileId(fileId).build();
    battleExecutionDao.update(battleExecutionEntity);
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
