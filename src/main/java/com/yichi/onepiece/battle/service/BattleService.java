package com.yichi.onepiece.battle.service;

import com.yichi.onepiece.battle.core.Battlefield;
import com.yichi.onepiece.battle.core.CharacterInBattle;
import com.yichi.onepiece.battle.model.*;
import com.yichi.onepiece.battle.model.BattleListView;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.model.BattleFullView;
import com.yichi.onepiece.quartz.jobs.ReportGeneratingJob;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import static com.yichi.onepiece.constants.OnepieceConstants.*;
import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.*;

@Service
public class BattleService {
  private static final ResourceType RESOURCE_TYPE = ResourceType.CHARACTER;
  private static final int BATTLE_MAXIMUM_TURNS = 120;
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
  private final Clock clock;

  public BattleService(
          BattleDao battleDao,
          BattleExecutionDao battleExecutionDao, SchedulerFactoryBean schedulerFactoryBean,
          CharacterService characterService,
          FruitService fruitService,
          ActionRecordService actionRecordService,
          BattleProcessDetailDao battleProcessDetailDao,
          BattleStatusDetailDao battleStatusDetailDao,
          UserService userService, Clock clock) {
    this.battleDao = battleDao;
    this.battleExecutionDao = battleExecutionDao;
    this.schedulerFactoryBean = schedulerFactoryBean;
    this.characterService = characterService;
    this.fruitService = fruitService;
    this.actionRecordService = actionRecordService;
    this.battleProcessDetailDao = battleProcessDetailDao;
    this.battleStatusDetailDao = battleStatusDetailDao;
    this.userService = userService;
      this.clock = clock;
  }

  public CreateBattleResponse createBattle(CreateBattleRequest request) {
    var actionRecord = actionRecordService.create();
    if (request.getStarterId().equals(request.getReceiverId())) {
      return new CreateBattleResponse().result(Boolean.FALSE).notification(
              UserFacingConstants.getConstant(SAME_STARTER_AND_RECEIVER).getDisplayName()
      );
    }
    var starter = characterService.getCharacterEntityById(request.getStarterId());
    var receiver = characterService.getCharacterEntityById(request.getReceiverId());
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
            .setStarterScoreBefore(starter.getScore())
            .setReceiverScoreBefore(receiver.getScore())
            .setBattleReportStage(BattleReportStage.WAITING)
            .creationTimestamp(actionRecord.getActionTimestamp())
            .modificationTimestamp(actionRecord.getActionTimestamp())
            .build();
    battleExecutionDao.insert(battleExecutionEntity);
    return new CreateBattleResponse().battleId(battleEntity.getId()).result(true).notification(
            UserFacingConstants.getConstant(CREATION_SUCCESS).getDisplayName()
    );
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
    battleFullViewEntity = battleFullViewEntity.toBuilder()
            .setNextBattle(getNext(battleFullViewEntity.getCreationRecord().getId()))
            .setPreviousBattle(getPrevious(battleFullViewEntity.getCreationRecord().getId()))
            .build();
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

  /**
   * This is the function where battle started to execute.
   * <p>
   * Step1: A battlefield will be created based on the two sides of participants.
   * Step2: A battle will get started when the starter initiate its attack.
   * Step3: A battle ends when energy/blood of any participant reaches 0, or less than 0.
   * Step4:
   *  - If the energy of any participant is less than 10% and this character has eaten any fruit, this fruit will
   *    not have any effect. Note: Paramecia fruit cost extra energy for every turn, the extra energy.
   *  - Paramecia may have defect on its component, this functionality will be added IN THE FUTURE.
   * Step5: Attacker and defender is interchangeable, who the character with  intelligence got more chances to attack
   * and character with higher speed get more chances of hiding from attack.
   * Step6: If battle ends, the battle score will change based on the battle result.
   *
   * @param battleId ID of battle.
   * @param userId User id of battle creator.
   * @param email Email address of creator.
   * @param nickname Nickname of creator.
   * @param roleName RoleName of creator.
   */
  public void startBattle(UUID battleId, UUID userId, String email, String nickname, String roleName, String locale) {
    var battleEntity = battleDao.findById(battleId).orElseThrow();
    var battlefield = createBattleField(battleId);
    int currentBattleTurn = 0;
    while (battlefield.allAlive()) {
      if (currentBattleTurn>BATTLE_MAXIMUM_TURNS) {
        var battleExecution = battleExecutionDao.findByBattleId(battleId).orElseThrow();
        var battleExecutionBuilder = battleExecution.toBuilder()
                .setBattleStatus(BattleStatus.FINISHED)
                .setModificationTimestamp(clock.millis())
                .setWinnerId(battlefield.getAttackSide().getBlood()>battlefield.getDefenseSide().getBlood()?
                        battlefield.getAttackSide().getCharacterId():
                        battlefield.getDefenseSide().getCharacterId());
        if (battleEntity.getStarterId().equals(battlefield.getAttackSide().getCharacterId())) {
          battleExecutionBuilder.setStarterScoreBefore(
                  battlefield.getAttackSide().getScore()).setStarterScoreAfter(battlefield.getAttackSide().getScore())
                  .setReceiverScoreBefore(
                          battlefield.getDefenseSide().getScore())
                  .setReceiverScoreAfter(battlefield.getDefenseSide().getScore());
        } else {
          battleExecutionBuilder.setStarterScoreBefore(
                          battlefield.getDefenseSide().getScore()).setStarterScoreAfter(battlefield.getDefenseSide().getScore())
                  .setReceiverScoreBefore(
                          battlefield.getAttackSide().getScore())
                  .setReceiverScoreAfter(battlefield.getAttackSide().getScore());
        }
        battleExecutionBuilder.setWinnerScoreChange(0);
        battleExecutionBuilder.setLoserScoreChange(0);
        battleExecutionBuilder.setBattleStatus(BattleStatus.FAILED);
        battleExecution = battleExecutionBuilder.build();
        battleExecutionDao.update(battleExecution);
      }
      battlefield.checkReverse();
      battlefield.processBattle();
      battlefield.refreshAttackSide();
      currentBattleTurn ++;
    }
    battlefield.completeFinalStatus();
    battleStatusDetailDao.batchInsert(battlefield.getBattleStatusDetailEntities());
    battleProcessDetailDao.batchInsert(battlefield.getBattleProcessDetailEntities());
    logger.info("Battle finish");
    var winner = battlefield.getWinner();
    var loser = battlefield.getLoser();
    // pair of [winner score, loser score]
    var scores = calculateCharacterScores(winner, loser);
    var battleExecution = battleExecutionDao.findByBattleId(battleId).orElseThrow();
    var battleExecutionBuilder = battleExecution.toBuilder()
            .setBattleStatus(BattleStatus.FINISHED)
            .setModificationTimestamp(clock.millis())
            .setWinnerId(winner.getCharacterId());
    if (battleEntity.getStarterId().equals(winner.getCharacterId())) {
      battleExecutionBuilder.setStarterScoreBefore(winner.getScore()).setStarterScoreAfter(scores.getLeft())
              .setReceiverScoreBefore(loser.getScore()).setReceiverScoreAfter(scores.getRight());
    } else {
      battleExecutionBuilder.setStarterScoreBefore(loser.getScore()).setStarterScoreAfter(scores.getRight())
              .setReceiverScoreBefore(winner.getScore()).setReceiverScoreAfter(scores.getLeft());
    }
    battleExecutionBuilder.setWinnerScoreChange(scores.getLeft()- winner.getScore());
    battleExecutionBuilder.setLoserScoreChange(scores.getRight()- loser.getScore());
    battleExecution = battleExecutionBuilder.build();
    battleExecutionDao.update(battleExecution);
    updateCharacter(winner.getCharacterId(), scores.getLeft(), userId, UserRole.fromValue(roleName));
    updateCharacter(loser.getCharacterId(), scores.getRight(), userId, UserRole.fromValue(roleName));
    scheduleFileGeneratingJob(battleId, userId, email, nickname, roleName, locale);
  }

  private void updateCharacter(UUID characterId, int newScore, UUID userId, UserRole loginRole) {
    var record = actionRecordService.createWithUserIdAndLoginRole(userId, loginRole);
    var character = characterService.findByCharacterId(characterId);
    character = character.toBuilder().setScore(newScore).setModificationRecordId(record.getId()).build();
    characterService.updateCharacterEntity(character);
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
   * @return pair of [winner final score, loser final score]
   */
  private Pair<Integer, Integer> calculateCharacterScores(CharacterInBattle winner, CharacterInBattle loser) {
    int winnerKFactor = getKFactorOfCharacter(winner.getCharacterId());
    int loserKFactor = getKFactorOfCharacter(loser.getCharacterId());

    int winnerScore = winner.getScore();
    int loserScore = loser.getScore();

    double expectedWinnerChange = 1.0 / (1.0 + Math.pow(10, (double) (loserScore - winnerScore) / 3000));
    double expectedLoserChange = 1.0 / (1.0 + Math.pow(10, (double) (winnerScore -loserScore) / 3000));

    int winnerRealChange = (int) (winnerKFactor*(1-expectedWinnerChange));
    int loserRealChange = (int) (loserKFactor*(0-expectedLoserChange));

    if (Math.abs(winnerRealChange) <5) {
      winnerRealChange = 0;
    }

    if (Math.abs(loserRealChange) <5) {
      loserRealChange = 0;
    }

    int updatedWinnerScore = winnerScore + winnerRealChange;
    int updatedLoserScore = loserScore + loserRealChange;

    int finalWinnerScore = roundScoreBoundary(updatedWinnerScore);
    int finalLoserScore = roundScoreBoundary(updatedLoserScore);

    return Pair.of(finalWinnerScore, finalLoserScore);
  }

  private int getKFactorOfCharacter(UUID characterId) {
    int battleCount = battleDao.listBattlesByCharacterId(characterId).size();
    if(battleCount<=10) {
      return 1500;
    }
    int newKFactor =  2000 - (battleCount-10)*30;
    if (newKFactor < 500) {
      newKFactor = 500;
    }
    return  newKFactor;
  }

  private int roundScoreBoundary(int score) {
    if (score<100) {
      return 100;
    }
    return Math.min(score, 10000);
  }

  private void scheduleFileGeneratingJob(UUID battleId, UUID userId, String email, String nickname, String roleName, String locale) {
    try {
      logger.info("start generating report");
      JobDataMap jobDataMap = new JobDataMap();
      jobDataMap.put(BATTLE_ID, battleId.toString());
      jobDataMap.put(USER_ID, userId.toString());
      jobDataMap.put(USER_EMAIL, email);
      jobDataMap.put(USER_NICKNAME, nickname);
      jobDataMap.put(ROLE_NAME, roleName);
      jobDataMap.put(LOCALE, locale);
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

  public BattleListView getPrevious(Long currentCreationRecordId) {
    var battleList =  battleDao.getPreviousBattleId(currentCreationRecordId);
    if (battleList.isEmpty()) {
      return null;
    }
    return battleDao.getBattleListView(battleList.get(0)).orElseThrow();
  }

  public BattleListView getNext(Long currentCreationRecordId) {
    var battleList =  battleDao.getNextBattleId(currentCreationRecordId);
    if (battleList.isEmpty()) {
      return null;
    }
    return battleDao.getBattleListView(battleList.get(0)).orElseThrow();
  }
}
