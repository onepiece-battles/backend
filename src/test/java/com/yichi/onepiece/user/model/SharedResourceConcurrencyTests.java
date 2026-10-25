package com.yichi.onepiece.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.testing.ConcurrencyDatabase;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

public class SharedResourceConcurrencyTests {
  private static final UUID SOURCE_ID = new UUID(0, 1);
  private static final String MIGRATION = "V000000025__deduplicate_and_constrain_user_resource.sql";

  @Test
  public void migrationRemovesInvalidAndDuplicateGrantsAndAddsRequiredConstraints() throws Exception {
    try (var database = database()) {
      var target = UUID.randomUUID();
      var resource = UUID.randomUUID();
      var first = UserResourceEntityBuilder.from(grant(target, resource, ResourceType.CHARACTER)).setId(new UUID(0, 2)).build();
      var duplicate = UserResourceEntityBuilder.from(grant(target, resource, ResourceType.CHARACTER)).setId(new UUID(0, 3)).build();
      var dao = database.mapper(UserResourceDao.class);
      dao.insertSharesIfAbsent(List.of(first, duplicate,
          grant(target, resource, ResourceType.FRUIT), grant(UUID.randomUUID(), resource, ResourceType.CHARACTER),
          grant(null, resource, ResourceType.CHARACTER), grant(target, null, ResourceType.CHARACTER),
          grant(target, resource, null)));
      database.migrate(MIGRATION);
      assertThat(count(database)).isEqualTo(3);
      try (var statement = database.connection.prepareStatement("SELECT id FROM user_resource WHERE user_id = ? AND resource_type = 'CHARACTER'")) {
        statement.setBytes(1, ConcurrencyDatabase.bytes(target));
        try (var rows = statement.executeQuery()) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getBytes(1)).isEqualTo(ConcurrencyDatabase.bytes(first.getId()));
        }
      }
      assertThatThrownBy(() -> {
        try (var statement = database.connection.prepareStatement("INSERT INTO user_resource VALUES (?, ?, ?, 'CHARACTER', ?)")) {
          statement.setBytes(1, ConcurrencyDatabase.bytes(UUID.randomUUID()));
          statement.setBytes(2, ConcurrencyDatabase.bytes(target));
          statement.setBytes(3, ConcurrencyDatabase.bytes(SOURCE_ID));
          statement.setBytes(4, ConcurrencyDatabase.bytes(resource));
          statement.executeUpdate();
        }
      }).isInstanceOf(java.sql.SQLIntegrityConstraintViolationException.class);
      assertThatThrownBy(() -> dao.insertSharesIfAbsent(List.of(grant(null, resource, ResourceType.CHARACTER))))
          .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
  }

  @Test
  public void concurrentSharesCreateOneGrantAndRetryDoesNotReplaceItsIdentity() throws Exception {
    try (var database = database()) {
      database.migrate(MIGRATION);
      var target = UUID.randomUUID();
      var resource = UUID.randomUUID();
      var factory = database.factory();
      var workers = Executors.newFixedThreadPool(2);
      var start = new CountDownLatch(1);
      try {
        java.util.concurrent.Callable<Integer> insert = () -> {
          start.await();
          try (var session = factory.openSession(true)) {
            return session.getMapper(UserResourceDao.class).insertSharesIfAbsent(
                List.of(grant(target, resource, ResourceType.CHARACTER)));
          }
        };
        var first = workers.submit(insert);
        var second = workers.submit(insert);
        start.countDown();
        first.get(5, TimeUnit.SECONDS);
        second.get(5, TimeUnit.SECONDS);
        assertThat(count(database)).isEqualTo(1);
        byte[] storedId;
        try (var statement = database.connection.createStatement(); var rows = statement.executeQuery("SELECT id FROM user_resource")) {
          assertThat(rows.next()).isTrue();
          storedId = rows.getBytes(1);
        }
        var dao = database.mapper(UserResourceDao.class);
        dao.insertSharesIfAbsent(List.of(grant(target, resource, ResourceType.CHARACTER),
            grant(target, UUID.randomUUID(), ResourceType.CHARACTER)));
        assertThat(count(database)).isEqualTo(2);
        try (var statement = database.connection.prepareStatement("SELECT id, source_user_id FROM user_resource WHERE resource_id = ?")) {
          statement.setBytes(1, ConcurrencyDatabase.bytes(resource));
          try (var rows = statement.executeQuery()) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getBytes(1)).isEqualTo(storedId);
            assertThat(rows.getBytes(2)).isEqualTo(ConcurrencyDatabase.bytes(SOURCE_ID));
          }
        }
      } finally {
        workers.shutdownNow();
      }
    }
  }

  @Test
  public void conflictHandlingDoesNotHideForeignKeyFailures() throws Exception {
    try (var database = database()) {
      database.migrate(MIGRATION);
      var invalid = UserResourceEntityBuilder.from(grant(UUID.randomUUID(), UUID.randomUUID(), ResourceType.GROUP))
          .setSourceUserId(UUID.randomUUID()).build();
      assertThatThrownBy(() -> database.mapper(UserResourceDao.class).insertSharesIfAbsent(List.of(invalid)))
          .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
      assertThat(count(database)).isZero();
    }
  }

  private ConcurrencyDatabase database() throws Exception {
    var database = new ConcurrencyDatabase();
    database.execute("CREATE TABLE share_source (id BINARY(16) PRIMARY KEY)");
    try (var statement = database.connection.prepareStatement("INSERT INTO share_source VALUES (?)")) {
      statement.setBytes(1, ConcurrencyDatabase.bytes(SOURCE_ID));
      statement.executeUpdate();
    }
    database.execute("CREATE TABLE user_resource (id BINARY(16) PRIMARY KEY, user_id BINARY(16), "
        + "source_user_id BINARY(16), resource_type VARCHAR(255), resource_id BINARY(16), "
        + "FOREIGN KEY (source_user_id) REFERENCES share_source(id))");
    database.configuration.addMapper(UserResourceDao.class);
    return database;
  }

  private UserResourceEntity grant(UUID target, UUID resource, ResourceType type) {
    return UserResourceEntityBuilder.newBuilder().setId(UUID.randomUUID()).setUserId(target)
        .setSourceUserId(SOURCE_ID).setResourceType(type).setResourceId(resource).build();
  }

  private int count(ConcurrencyDatabase database) throws Exception {
    try (var statement = database.connection.createStatement(); var rows = statement.executeQuery("SELECT COUNT(*) FROM user_resource")) {
      rows.next();
      return rows.getInt(1);
    }
  }
}
