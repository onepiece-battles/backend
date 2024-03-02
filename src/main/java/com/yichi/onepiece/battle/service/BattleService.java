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
import com.yichi.onepiece.quartz.jobs.ReportGeneratingJob;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import java.util.List;
import java.util.UUID;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import static com.yichi.onepiece.constants.OnepieceConstants.*;

@Service
public class BattleService {
  private static final ResourceType RESOURCE_TYPE = ResourceType.CHARACTER;
  Logger logger = LoggerFactory.getLogger(BattleService.class);
  private final BattleDao battleDao;
  private final BattleExecutionDao battleExecutionDao;
  private final SchedulerFactoryBean schedulerFactoryBean;
  private final CharacterService characterService;
  private final FruitService fruitService;
  private final ActionRecordService actionRecordService;
  private final BattleProcessDetailDao battleProcessDetailDao;
  private final BattleStatusDetailDao battleStatusDetailDao;
  private final UserService userService;

  public BattleService(
          BattleDao battleDao,
          BattleExecutionDao battleExecutionDao, SchedulerFactoryBean schedulerFactoryBean,
          CharacterService characterService,
          FruitService fruitService,
          ActionRecordService actionRecordService,
          BattleProcessDetailDao battleProcessDetailDao,
          BattleStatusDetailDao battleStatusDetailDao,
          UserService userService) {
    this.battleDao = battleDao;
    this.battleExecutionDao = battleExecutionDao;
    this.schedulerFactoryBean = schedulerFactoryBean;
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
            // this service focus on
            .setType(BattleType.SINGLE)
            .setSystemResource(UserHelper.hasNikaPermission())
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
        .type(insertedEntity.getType())
        .starterId(insertedEntity.getStarterId())
        .receiverId(insertedEntity.getReceiverId());
  }

  public ListBattleResponse list(ListBattleRequest request) {
    var userId = UserHelper.getCurrentUserId();
    var resourceIds = userService.getResourceIdsByUserIdResourceType(userId, RESOURCE_TYPE);
    var totalSize = battleDao.searchCount(userId, request.getBattleFilter(), resourceIds);
    logger.info("total size {}", totalSize);
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

  public BattleExecution getBattleExecution(GetBattleExecutionRequest request) {
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

  public void startBattle(UUID battleId, UUID userId, String email, String nickname, String roleName) {
    var battlefield = createBattleField(battleId);
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
    updateCharacterScores(winner, loser);
    var battleExecution = battleExecutionDao.findByBattleId(battleId).orElseThrow();
    battleExecution = battleExecution.toBuilder()
            .setBattleStatus(BattleStatus.FINISHED)
            .setWinnerId(winner.getCharacterId())
            .build();
    battleExecutionDao.update(battleExecution);
    logger.info("Winner is {}", winner.getName());
    logger.info("Loser is {}", loser.getName());
    scheduleFileGeneratingJob(battleId, userId, email, nickname, roleName);
  }

  /**
   * Calculate the new score of two participants.
   * [K value description]
   * K value here depends on how many battles this participant has already conducted.
   * The range of K value is [16-32]
   * The more battles this character has participated in battle, the smaller the k value is for this character.
   * 1. For characters participated in less or equal than 10 battles, the k value is 32.
   * 2. For every more 10 battles this character conducted, the K value decrease by 1.
   *
   * @param winner Winner of the battle
   * @param loser Loser of the battle
   */
  private void updateCharacterScores(CharacterInBattle winner, CharacterInBattle loser) {
    var winnerEntity = characterService.findByCharacterId(winner.getCharacterId());
    var loserEntity = characterService.findByCharacterId(loser.getCharacterId());

    int winnerKFactor = getKFactorOfCharacter(winner.getCharacterId());
    int loserKFactor = getKFactorOfCharacter(loser.getCharacterId());

    int winnerScore = winnerEntity.getScore();
    int loserScore = loserEntity.getScore();

    double diffFactor = getDiffFactor(winnerScore, loserScore);
    logger.info("diff factor: {}", diffFactor);

    logger.info("winner score:{}, loser score: {}", winnerScore, loserScore);

    double expectedWinnerChange = 1.0 / (1.0 + Math.pow(10, (double) (loserScore - winnerScore  ) / 1000));
    double expectedLoserChange = 1.0 / (1.0 + Math.pow(10, (double) (winnerScore -loserScore) / 1000));

    logger.info("winner change:{}, loser change: {}", expectedWinnerChange, expectedLoserChange);
    logger.info("winner real change:{}, loser real change: {}", diffFactor*winnerKFactor*(1-expectedWinnerChange), diffFactor*loserKFactor*(0-expectedLoserChange));

    double updatedWinnerScore = winnerScore + diffFactor*winnerKFactor*(1-expectedWinnerChange);
    double updatedLoserScore = loserScore + diffFactor*loserKFactor*(0-expectedLoserChange);

    logger.info("updated winner score: {}, updated loser score: {}", updatedWinnerScore, updatedLoserScore);

    int finalWinnerScore = roundScoreBoundary(updatedWinnerScore);
    int finalLoserScore = roundScoreBoundary(updatedLoserScore);
    logger.info("final winner score: {}, final loser score: {}", finalWinnerScore, finalLoserScore);
    winnerEntity = winnerEntity.toBuilder().setScore(finalWinnerScore).build();
    loserEntity = loserEntity.toBuilder().setScore(finalLoserScore).build();

    characterService.updateCharacterEntity(winnerEntity);
    characterService.updateCharacterEntity(loserEntity);
  }

  private int getKFactorOfCharacter(UUID characterId) {
    int battleCount = battleDao.listBattlesByCharacterId(characterId).size();
    if(battleCount<=10) {
      return 320;
    }
    int newKFactor =  320 - (battleCount-10);
    if (newKFactor < 160) {
      newKFactor = 160;
    }
    return  newKFactor;
  }

  private double getDiffFactor(int winnerScore, int loserScore) {
    var factor =  (double) (Math.abs(winnerScore - loserScore) * 5) /(winnerScore+loserScore);
    if (factor < 1) {
      return 1;
    }
    return factor;
  }

  private int roundScoreBoundary(double score) {
    if (score<100) {
      return 100;
    }
    return (int) Math.min(score, 10000);
  }

  private void scheduleFileGeneratingJob(UUID battleId, UUID userId, String email, String nickname, String roleName) {
    try {
      logger.info("start generating report");
      JobDataMap jobDataMap = new JobDataMap();
      jobDataMap.put(BATTLE_ID, battleId.toString());
      jobDataMap.put(USER_ID, userId.toString());
      jobDataMap.put(USER_EMAIL, email);
      jobDataMap.put(USER_NICKNAME, nickname);
      jobDataMap.put(ROLE_NAME, roleName);
      JobDetail reportGeneratingJob = JobBuilder.newJob(ReportGeneratingJob.class)
              .setJobData(jobDataMap)
              .withIdentity(UUID.randomUUID().toString(), BATTLE_JOB_GROUP)
              .withDescription("generating battle pdf report")
              .build();
      Scheduler scheduler = schedulerFactoryBean.getScheduler();
      Trigger reportGeneratingTrigger = TriggerBuilder.newTrigger()
              .forJob(reportGeneratingJob)
              .startNow().build();
      scheduler.start();
      scheduler.scheduleJob(reportGeneratingJob, reportGeneratingTrigger);
      updateBattleReportStage(battleId, BattleReportStage.IN_PROGRESS);
    } catch (Exception e) {
      logger.error(e.toString());
    }
  }

  public void updateBattleReportStage(UUID battleId, BattleReportStage battleReportStage) {
    logger.info("update battle report stage to {}", battleReportStage);
    var battleExecution = battleExecutionDao.findByBattleId(battleId).orElseThrow();
    battleExecution = battleExecution.toBuilder().setBattleReportStage(battleReportStage).build();
    battleExecutionDao.update(battleExecution);
  }

  Battlefield createBattleField(UUID battleId) {
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
    return new Battlefield(characterInBattleInit, characterInBattleReceive, battleId);
  }

  // update pdf file id
  public void updateFileId(UUID battleId, UUID fileId) {
    logger.info("update file id");
    var battleExecutionEntity = battleExecutionDao.findByBattleId(battleId).orElseThrow();
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
