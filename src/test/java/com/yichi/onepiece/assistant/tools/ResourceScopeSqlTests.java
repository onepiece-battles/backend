package com.yichi.onepiece.assistant.tools;

import static org.assertj.core.api.Assertions.assertThat;

import com.yichi.onepiece.battle.model.BattleDao;
import com.yichi.onepiece.character.model.dao.CharacterDao;
import com.yichi.onepiece.fruit.model.FruitDao;
import com.yichi.onepiece.group.model.GroupDao;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.testing.ConcurrencyDatabase;
import java.nio.file.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testng.annotations.Test;

/** Executes the real mapper filters, including all three OR branches, against a database. */
public class ResourceScopeSqlTests {
  @Test
  public void ownSystemAndSharedFiltersAreValidAndStrictScopesExcludeSharedRows() throws Exception {
    try (var db = new ConcurrencyDatabase()) {
      UUID user = UUID.randomUUID(), other = UUID.randomUUID();
      db.execute(
          "CREATE TABLE action_lineage(id BIGINT PRIMARY KEY,user_id BINARY(16),role_id"
              + " BINARY(16))");
      db.execute(
          "CREATE TABLE `character`(id BINARY(16) PRIMARY KEY,name VARCHAR(100),creation_record_id"
              + " BIGINT,system_resource BOOLEAN)");
      db.execute(
          "CREATE TABLE fruit(id BINARY(16) PRIMARY KEY,creation_record_id BIGINT,system_resource"
              + " BOOLEAN)");
      db.execute(
          "CREATE TABLE `group`(id BINARY(16) PRIMARY KEY,creation_record_id BIGINT,system_resource"
              + " BOOLEAN)");
      db.execute(
          "CREATE TABLE battle(id BINARY(16) PRIMARY KEY,creation_record_id BIGINT,starter_id"
              + " BINARY(16),receiver_id BINARY(16),system_resource BOOLEAN)");
      db.execute("CREATE TABLE battle_execution(battle_id BINARY(16))");
      db.execute("CREATE TABLE character_to_fruit(character_id BINARY(16),fruit_id BINARY(16))");
      db.execute("CREATE TABLE haki(character_id BINARY(16))");
      var jdbc = new JdbcTemplate(db.dataSource());
      jdbc.update(
          "INSERT INTO action_lineage VALUES(1,?,?)",
          ConcurrencyDatabase.bytes(user),
          ConcurrencyDatabase.bytes(UUID.randomUUID()));
      jdbc.update(
          "INSERT INTO action_lineage VALUES(2,?,?)",
          ConcurrencyDatabase.bytes(other),
          ConcurrencyDatabase.bytes(UUID.randomUUID()));
      UUID shared = UUID.randomUUID();
      for (String table : List.of("character", "fruit", "group", "battle")) {
        for (int i = 0; i < 4; i++) {
          UUID id = i == 2 ? shared : UUID.randomUUID();
          jdbc.update(
              "INSERT INTO `" + table + "` (id,creation_record_id,system_resource) VALUES(?,?,?)",
              ConcurrencyDatabase.bytes(id),
              i == 0 ? 1 : 2,
              i == 1);
        }
      }

      for (String path :
          List.of(
              "character/model/dao/characterDao.xml",
              "fruit/model/fruitDao.xml",
              "group/model/groupDao.xml",
              "battle/model/battleDao.xml")) {
        String resource = "src/main/resources/com/yichi/onepiece/" + path;
        new org.apache.ibatis.builder.xml.XMLMapperBuilder(
                Files.newInputStream(Path.of(resource)),
                db.configuration,
                "com/yichi/onepiece/" + path,
                db.configuration.getSqlFragments())
            .parse();
      }
      var chars = db.mapper(CharacterDao.class);
      var fruits = db.mapper(FruitDao.class);
      var groups = db.mapper(GroupDao.class);
      var battles = db.mapper(BattleDao.class);
      assertThat(
              chars.count(new CharacterFilter().userId(user).systemResource(true), List.of(shared)))
          .isEqualTo(3);
      assertThat(chars.count(new CharacterFilter().userId(user), List.of())).isEqualTo(1);
      assertThat(chars.count(new CharacterFilter().systemResource(true), List.of())).isEqualTo(1);
      assertThat(
              fruits.searchCount(
                  user, new FruitFilter().userId(user).systemResource(true), List.of(shared)))
          .isEqualTo(3);
      assertThat(fruits.searchCount(user, new FruitFilter().userId(user), List.of())).isEqualTo(1);
      assertThat(
              groups.count(
                  new GroupFilter().userId(user).systemResource(true), user, List.of(shared)))
          .isEqualTo(3);
      assertThat(groups.count(new GroupFilter().systemResource(true), user, List.of()))
          .isEqualTo(1);
      assertThat(
              battles.searchCount(
                  new BattleFilter().userId(user).systemResource(true), List.of(shared)))
          .isEqualTo(3);
      assertThat(battles.searchCount(new BattleFilter().userId(user), List.of())).isEqualTo(1);
    }
  }
}
