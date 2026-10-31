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

  private Profile legacy(String identity) throws Exception {
    try (var stream = getClass().getResourceAsStream("/battle/catalog-v1.json")) {
      return new ObjectMapper()
          .readValue(stream, Catalog.class).characters().stream()
              .filter(c -> c.code().equals(identity))
              .findFirst()
              .orElseThrow()
              .profile();
    }
  }

  @Test
  public void untouchedStoredPresetsUpgradeByIdentityWithoutRewritingPastBattles()
      throws Exception {
    try (var db = database()) {
      UUID id = UUID.randomUUID(), battle = UUID.randomUUID();
      insert(db, "`character`", id);
      insert(db, "battle", battle);
      var dao = db.mapper(CombatProfileDao.class);
      var records = db.mapper(CombatRecordDao.class);
      var before = legacy("luffy");
      String frozen = new ObjectMapper().writeValueAsString(before);
      dao.insert(id, 1, frozen, 1);
      records.insert(battle, "skills-v1", 42, frozen, 1);
      var profiles =
          db.transactional(
              new CombatProfiles(dao, new CombatCatalog(), Clock.systemUTC()),
              CombatProfiles.class);
      var renamed = CombatEngineTests.character(id, "手动改过的名字");
      assertThat(profiles.preview(renamed).version()).isEqualTo(2);
      assertThat(dao.history(id)).hasSize(1); // Reading a detail page never writes.
      var next = profiles.freeze(renamed);
      assertThat(next.version()).isEqualTo(2);
      assertThat(next.identity()).isEqualTo("luffy");
      assertThat(next.skills()).anyMatch(s -> s.id().equals("luffy.gear-five"));
      assertThat(profiles.freeze(renamed)).isEqualTo(next);
      assertThat(dao.history(id)).hasSize(2);
      assertThat(new ObjectMapper().readValue(dao.history(id).get(0), Profile.class))
          .isEqualTo(before);
      assertThat(records.snapshot(battle)).contains(frozen);
    }
  }

  @Test
  public void customizedProfilesAndUserResourcesAreNeverReplacedByCatalogueUpdates()
      throws Exception {
    try (var db = database()) {
      var dao = db.mapper(CombatProfileDao.class);
      var profiles =
          db.transactional(
              new CombatProfiles(dao, new CombatCatalog(), Clock.systemUTC()),
              CombatProfiles.class);
      var before = legacy("luffy");
      var edited =
          new Profile(
              1,
              before.identity(),
              "手工配置时期",
              before.coatingLevel(),
              before.skills(),
              before.evidence());
      var published =
          new Profile(
              2,
              before.identity(),
              before.era(),
              before.coatingLevel(),
              before.skills(),
              before.evidence());
      for (var profile : List.of(edited, published, before)) {
        UUID id = UUID.randomUUID();
        insert(db, "`character`", id);
        dao.insert(id, profile.version(), new ObjectMapper().writeValueAsString(profile), 1);
        var character =
            CombatEngineTests.character(id, "路飞").toBuilder()
                .setSystemResource(profile != before)
                .build();
        assertThat(profiles.preview(character)).isEqualTo(profile);
        assertThat(profiles.freeze(character)).isEqualTo(profile);
        assertThat(dao.history(id)).hasSize(1);
      }
    }
  }

  @Test
  public void concurrentBattlesAppendOnlyOnePresetUpgrade() throws Exception {
    try (var db = database()) {
      UUID id = UUID.randomUUID();
      insert(db, "`character`", id);
      var dao = db.mapper(CombatProfileDao.class);
      dao.insert(id, 1, new ObjectMapper().writeValueAsString(legacy("luffy")), 1);
      var profiles =
          db.transactional(
              new CombatProfiles(dao, new CombatCatalog(), Clock.systemUTC()),
              CombatProfiles.class);
      var executor = Executors.newFixedThreadPool(2);
      var start = new CountDownLatch(1);
      try {
        Callable<Profile> freeze =
            () -> {
              start.await();
              return profiles.freeze(CombatEngineTests.character(id, "路飞"));
            };
        var a = executor.submit(freeze);
        var b = executor.submit(freeze);
        start.countDown();
        assertThat(a.get(5, TimeUnit.SECONDS).version()).isEqualTo(2);
        assertThat(b.get(5, TimeUnit.SECONDS).version()).isEqualTo(2);
        assertThat(dao.history(id)).hasSize(2);
      } finally {
        executor.shutdownNow();
      }
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
              first.version() + 1,
              first.identity(),
              first.era(),
              first.coatingLevel(),
              first.skills().subList(0, 1),
              first.evidence());
      profiles.publish(id, first.version(), next);
      assertThat(profiles.preview(character)).isEqualTo(next);
      assertThat(json.readValue(records.snapshot(battle).orElseThrow(), Profile.class))
          .isEqualTo(first);
      assertThatThrownBy(() -> profiles.publish(id, first.version(), next))
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
          first.version(),
          new Profile(
              first.version() + 1,
              first.identity(),
              first.era(),
              first.coatingLevel(),
              List.of(),
              first.evidence()));
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
                      first.version() + 1,
                      new Profile(
                          first.version() + 2,
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
                      first.version() + 1,
                      new Profile(
                          first.version() + 2,
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
