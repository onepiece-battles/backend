package com.yichi.onepiece.battle.skill;

import static com.yichi.onepiece.battle.skill.CombatModels.*;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.testing.ConcurrencyDatabase;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import org.testng.annotations.Test;

public class CombatPersistenceTests {
  private ConcurrencyDatabase database() throws Exception {
    var db = new ConcurrencyDatabase();
    db.execute(
        "CREATE TABLE `character`(id BINARY(16) PRIMARY KEY, system_resource BOOLEAN,"
            + " creation_record_id BIGINT)");
    db.execute("CREATE TABLE battle(id BINARY(16) PRIMARY KEY)");
    db.migrate("V000000032__versioned_battle_skills.sql");
    db.configuration.addMapper(CombatProfileDao.class);
    db.configuration.addMapper(CombatRecordDao.class);
    return db;
  }

  private void insert(ConcurrencyDatabase db, String table, UUID id) throws Exception {
    try (PreparedStatement statement =
        db.connection.prepareStatement("INSERT INTO " + table + "(id) VALUES(?)")) {
      statement.setBytes(1, ConcurrencyDatabase.bytes(id));
      statement.executeUpdate();
    }
  }

  @Test
  public void aProfileIsBoundToIdAndPublishedVersionsCannotRewriteEarlierSnapshots()
      throws Exception {
    try (var db = database()) {
      UUID id = UUID.randomUUID(), battle = UUID.randomUUID();
      insert(db, "`character`", id);
      insert(db, "battle", battle);
      var dao = db.mapper(CombatProfileDao.class);
      var records = db.mapper(CombatRecordDao.class);
      var profiles =
          db.transactional(
              new CombatProfiles(dao, new CombatCatalog(), Clock.systemUTC()),
              CombatProfiles.class);
      var character = CombatEngineTests.character(id, "蒙奇·D·路飞");
      var first = profiles.freeze(character);
      assertThat(first.identity()).isEqualTo("luffy");
      assertThat(
              profiles.freeze(
                  character.toBuilder()
                      .setName(new com.yichi.onepiece.model.I18nText().zhCN("新名称"))
                      .build()))
          .isEqualTo(first);
      var json = new ObjectMapper();
      String snapshot = json.writeValueAsString(first);
      records.insert(battle, ENGINE_VERSION, 42, snapshot, 1);
      var next =
          new Profile(
              2,
              first.identity(),
              first.era(),
              first.coatingLevel(),
              first.skills().subList(0, 1),
              first.evidence());
      profiles.publish(id, 1, next);
      assertThat(profiles.preview(character)).isEqualTo(next);
      assertThat(json.readValue(records.snapshot(battle).orElseThrow(), Profile.class))
          .isEqualTo(first);
      assertThatThrownBy(() -> profiles.publish(id, 1, next))
          .isInstanceOf(IllegalArgumentException.class);
      assertThat(records.complete(battle, "[]")).isEqualTo(1);
      assertThat(records.complete(battle, "[{\"changed\":true}]")).isZero();
      assertThat(records.events(battle)).contains("[]");
      assertThat(dao.latest(id)).isPresent();
    }
  }

  @Test
  public void concurrentFirstBattlesCreateOneStableProfileVersion() throws Exception {
    try (var db = database()) {
      UUID id = UUID.randomUUID();
      insert(db, "`character`", id);
      var dao = db.mapper(CombatProfileDao.class);
      var profiles =
          db.transactional(
              new CombatProfiles(dao, new CombatCatalog(), Clock.systemUTC()),
              CombatProfiles.class);
      var executor = Executors.newFixedThreadPool(2);
      var start = new CountDownLatch(1);
      try {
        Callable<Profile> create =
            () -> {
              start.await();
              return profiles.freeze(CombatEngineTests.character(id, "马尔高"));
            };
        var a = executor.submit(create);
        var b = executor.submit(create);
        start.countDown();
        assertThat(a.get(5, TimeUnit.SECONDS)).isEqualTo(b.get(5, TimeUnit.SECONDS));
        try (var statement = db.connection.createStatement();
            var rows = statement.executeQuery("SELECT COUNT(*) FROM character_combat_profile")) {
          rows.next();
          assertThat(rows.getInt(1)).isEqualTo(1);
        }
      } finally {
        executor.shutdownNow();
      }
    }
  }

  @Test
  public void userCreatedNamesDoNotAutomaticallyReceiveCanonicalPowers() throws Exception {
    try (var db = database()) {
      var profiles =
          new CombatProfiles(
              db.mapper(CombatProfileDao.class), new CombatCatalog(), Clock.systemUTC());
      var character =
          CombatEngineTests.character(UUID.randomUUID(), "路飞").toBuilder()
              .setSystemResource(false)
              .build();
      assertThat(profiles.preview(character).skills()).isEmpty();
    }
  }

  @Test
  public void skillDefinitionEditsMustAdvanceTheirOwnVersionEvenAfterRemoval() throws Exception {
    try (var db = database()) {
      UUID id = UUID.randomUUID();
      insert(db, "`character`", id);
      var dao = db.mapper(CombatProfileDao.class);
      var profiles =
          db.transactional(
              new CombatProfiles(dao, new CombatCatalog(), Clock.systemUTC()),
              CombatProfiles.class);
      var first = profiles.freeze(CombatEngineTests.character(id, "路飞"));
      var skill = first.skills().get(0);
      profiles.publish(
          id,
          1,
          new Profile(
              2, first.identity(), first.era(), first.coatingLevel(), List.of(), first.evidence()));
      var changed =
          new Skill(
              skill.id(),
              1,
              skill.name() + "改",
              skill.englishName(),
              skill.origin(),
              skill.target(),
              skill.energy(),
              skill.cooldown(),
              skill.maxUses(),
              skill.healthBelow(),
              skill.requiredFruits(),
              skill.effects(),
              skill.evidence(),
              skill.tags());
      assertThatThrownBy(
              () ->
                  profiles.publish(
                      id,
                      2,
                      new Profile(
                          3,
                          first.identity(),
                          first.era(),
                          first.coatingLevel(),
                          List.of(changed),
                          first.evidence())))
          .isInstanceOf(IllegalArgumentException.class);
      var v2 =
          new Skill(
              skill.id(),
              2,
              changed.name(),
              skill.englishName(),
              skill.origin(),
              skill.target(),
              skill.energy(),
              skill.cooldown(),
              skill.maxUses(),
              skill.healthBelow(),
              skill.requiredFruits(),
              skill.effects(),
              skill.evidence(),
              skill.tags());
      assertThat(
              profiles
                  .publish(
                      id,
                      2,
                      new Profile(
                          3,
                          first.identity(),
                          first.era(),
                          first.coatingLevel(),
                          List.of(v2),
                          first.evidence()))
                  .skills()
                  .get(0)
                  .version())
          .isEqualTo(2);
    }
  }

  @Test
  public void failedFinalizationRollsBackEventsAndScoresAndCompletedResultsAreNotWrittenTwice()
      throws Exception {
    try (var db = database()) {
      UUID id = UUID.randomUUID();
      insert(db, "battle", id);
      db.execute("CREATE TABLE combat_score_fixture(value INT)");
      db.execute("INSERT INTO combat_score_fixture VALUES(0)");
      db.configuration.addMapper(ScoreFixture.class);
      var scores = db.mapper(ScoreFixture.class);
      var records = db.mapper(CombatRecordDao.class);
      records.insert(id, ENGINE_VERSION, 1, "{}", 1);
      var input = org.mockito.Mockito.mock(CombatInputs.class);
      var service =
          db.transactional(
              new CombatSession(
                  input,
                  org.mockito.Mockito.mock(BattleTactics.class),
                  records,
                  Clock.systemUTC(),
                  org.mockito.Mockito.mock(
                      com.yichi.onepiece.battle.model.BattleProcessDetailDao.class),
                  org.mockito.Mockito.mock(
                      com.yichi.onepiece.battle.model.BattleStatusDetailDao.class)),
              CombatSession.class);
      var field =
          new com.yichi.onepiece.battle.core.Battlefield(
              CombatEngineTests.actor(CombatEngineTests.A, CombatEngineTests.profile(0), List.of()),
              CombatEngineTests.actor(CombatEngineTests.B, CombatEngineTests.profile(0), List.of()),
              id);
      field.processBattle();
      field.completeFinalStatus();
      assertThatThrownBy(
              () ->
                  service.complete(
                      id,
                      field,
                      () -> {
                        scores.increment();
                        throw new IllegalStateException("failure after score update");
                      }))
          .isInstanceOf(IllegalStateException.class);
      assertThat(records.events(id)).isEmpty();
      assertThat(scores.value()).isZero();
      service.complete(id, field, scores::increment);
      assertThat(scores.value()).isEqualTo(1);
      assertThatThrownBy(() -> service.complete(id, field, scores::increment))
          .isInstanceOf(IllegalStateException.class);
      assertThat(scores.value()).isEqualTo(1);
    }
  }

  public interface ScoreFixture {
    @org.apache.ibatis.annotations.Update("UPDATE combat_score_fixture SET value=value+1")
    void increment();

    @org.apache.ibatis.annotations.Select("SELECT value FROM combat_score_fixture")
    int value();
  }
}
