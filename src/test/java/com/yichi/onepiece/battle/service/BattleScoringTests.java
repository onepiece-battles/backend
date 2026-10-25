package com.yichi.onepiece.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.battle.model.BattleDao;
import java.sql.DriverManager;
import java.util.UUID;
import org.apache.ibatis.annotations.Select;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class BattleScoringTests {
  @Test
  public void countsStarterAndReceiverBattlesIncludingMissingExecutions() throws Exception {
    String query = String.join(" ", BattleDao.class
        .getMethod("countBattlesByCharacterId", UUID.class).getAnnotation(Select.class).value())
        .replace("#{characterId}", "?");
    try (var connection = DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL");
        var statement = connection.createStatement()) {
      statement.execute("create table battle (id varchar primary key, starter_id varchar, receiver_id varchar)");
      statement.execute("create table battle_execution (battle_id varchar)");
      statement.execute("insert into battle values ('b1', 'c1', 'c2'), ('b2', 'c3', 'c1'), ('b3', 'c2', 'c3')");
      statement.execute("insert into battle_execution values ('b1'), ('b3')");
      try (var count = connection.prepareStatement(query)) {
        count.setString(1, "c1");
        try (var result = count.executeQuery()) {
          assertThat(result.next()).isTrue();
          assertThat(result.getInt(1)).isEqualTo(2);
        }
        count.setString(1, "missing");
        try (var result = count.executeQuery()) {
          assertThat(result.next()).isTrue();
          assertThat(result.getInt(1)).isZero();
        }
      }
    }
  }

  @DataProvider
  public Object[][] scoringTiers() {
    return new Object[][] {{0, 1500}, {10, 1500}, {11, 1970}, {59, 530}, {60, 500}, {10000, 500}};
  }

  @Test(dataProvider = "scoringTiers")
  public void preservesScoringTiersWithoutLoadingBattleHistory(int count, int expectedFactor) {
    var dao = mock(BattleDao.class);
    var service = new BattleService(dao, null, null, null, null, null, null, null, null, null);
    var characterId = UUID.randomUUID();
    when(dao.countBattlesByCharacterId(characterId)).thenReturn(count);
    Integer factor = ReflectionTestUtils.invokeMethod(service, "getKFactorOfCharacter", characterId);
    assertThat(factor).isEqualTo(expectedFactor);
    verify(dao).countBattlesByCharacterId(characterId);
    verifyNoMoreInteractions(dao);
  }
}
