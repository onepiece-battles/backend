package com.yichi.onepiece.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.testng.SkipException;
import org.testng.annotations.Test;

public class MySqlMigrationTests {
  @Test
  public void migratesEmptyDatabaseThroughFullHistoryAndValidatesChecksums() throws Exception {
    if (System.getenv("ONEPIECE_TEST_MYSQL_PORT") == null) {
      throw new SkipException("Set ONEPIECE_TEST_MYSQL_PORT and ONEPIECE_TEST_MYSQL_PASSWORD for disposable local MySQL");
    }
    try (var database = new ConcurrencyDatabase()) {
      var flyway = Flyway.configure().dataSource(database.dataSource()).locations("classpath:db/migration").load();
      flyway.migrate();
      assertThat(flyway.info().current().getVersion().compareTo(org.flywaydb.core.api.MigrationVersion.fromVersion("30"))).isZero();
      assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
      assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
  }
}
