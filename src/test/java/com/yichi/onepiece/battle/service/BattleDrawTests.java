package com.yichi.onepiece.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.battle.model.BattleDao;
import com.yichi.onepiece.battle.model.BattleEntity;
import com.yichi.onepiece.battle.model.BattleExecutionDao;
import com.yichi.onepiece.battle.model.BattleExecutionEntity;
import com.yichi.onepiece.battle.model.BattleExecutionEntityBuilder;
import com.yichi.onepiece.battle.model.BattleProcessDetailDao;
import com.yichi.onepiece.battle.model.BattleStatusDetailDao;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.model.BattleReportStage;
import com.yichi.onepiece.model.BattleStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

/** Two characters that cannot hurt each other reach the turn limit: the battle is a draw. */
public class BattleDrawTests {
  @Test
  public void reachingTheTurnLimitRecordsADrawWithoutWinnerOrScoreChange() {
    var battleDao = mock(BattleDao.class);
    var executionDao = mock(BattleExecutionDao.class);
    var characters = mock(CharacterService.class);
    var fruits = mock(FruitService.class);
    var processDao = mock(BattleProcessDetailDao.class);
    var statusDao = mock(BattleStatusDetailDao.class);
    var clock = Clock.fixed(Instant.ofEpochMilli(5_000), ZoneOffset.UTC);
    // No scheduler, action records or users: a draw must not touch scores or reports.
    var service = new BattleService(battleDao, executionDao, null, characters, fruits, null,
        processDao, statusDao, null, clock);

    UUID battleId = UUID.randomUUID(), garpId = UUID.randomUUID(), kaidoId = UUID.randomUUID();
    var garp = tank(garpId, 1800);
    var kaido = tank(kaidoId, 2100);
    var battle = mock(BattleEntity.class);
    when(battle.getId()).thenReturn(battleId);
    when(battle.getStarterId()).thenReturn(garpId);
    when(battle.getReceiverId()).thenReturn(kaidoId);
    when(battleDao.findById(battleId)).thenReturn(Optional.of(battle));
    when(characters.getCharacterEntityById(garpId)).thenReturn(garp);
    when(characters.getCharacterEntityById(kaidoId)).thenReturn(kaido);
    when(fruits.listFruitEntityByCharacterId(any())).thenReturn(List.of());
    when(executionDao.findByBattleId(battleId)).thenReturn(Optional.of(
        BattleExecutionEntityBuilder.newBuilder()
            .setBattleId(battleId)
            .setBattleStatus(BattleStatus.IN_PROGRESS)
            .setBattleReportStage(BattleReportStage.WAITING)
            .setStarterScoreBefore(1800)
            .setReceiverScoreBefore(2100)
            .build()));

    service.startBattle(battleId, UUID.randomUUID(), "a@b.test", "nika", "NIKA", "zh_CN");

    var saved = ArgumentCaptor.forClass(BattleExecutionEntity.class);
    verify(executionDao).update(saved.capture());
    var draw = saved.getValue();
    assertThat(draw.getBattleStatus()).isEqualTo(BattleStatus.FAILED);
    assertThat(draw.getWinnerId()).isNull();
    assertThat(draw.getStarterScoreAfter()).isEqualTo(draw.getStarterScoreBefore()).isEqualTo(1800);
    assertThat(draw.getReceiverScoreAfter()).isEqualTo(draw.getReceiverScoreBefore()).isEqualTo(2100);
    assertThat(draw.getWinnerScoreChange()).isZero();
    assertThat(draw.getLoserScoreChange()).isZero();
    assertThat(draw.getBattleReportStage()).isEqualTo(BattleReportStage.WAITING);
    // The rounds are kept so the draw can be replayed.
    verify(processDao).batchInsert(argThat(rounds -> rounds.size() > 100));
    verify(statusDao).batchInsert(argThat(rounds -> rounds.size() > 100));
    verify(characters, never()).updateCharacterEntity(any());
  }

  /** Defense above the attack on both sides and plenty of energy: nobody ever falls. */
  private static CharacterEntity tank(UUID id, int score) {
    var character = mock(CharacterEntity.class);
    when(character.getId()).thenReturn(id);
    when(character.getScore()).thenReturn(score);
    when(character.getAttack()).thenReturn(100);
    when(character.getDefense()).thenReturn(200);
    when(character.getBlood()).thenReturn(1000);
    when(character.getEnergy()).thenReturn(10_000);
    when(character.getIntelligence()).thenReturn(150);
    when(character.getSpeed()).thenReturn(100);
    return character;
  }
}
