package com.yichi.onepiece.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.model.UpdatePasswordRequest;
import com.yichi.onepiece.model.VerifyByEmailCodeRequest;
import com.yichi.onepiece.testing.ConcurrencyDatabase;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.EmailVerificationDao;
import com.yichi.onepiece.user.model.UserDao;
import com.yichi.onepiece.user.model.UserEntityBuilder;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class VerificationConcurrencyTests {
  private static final String EMAIL = "test@example.test";
  private static final String CODE = "123456";
  private static final long NOW = 1000;

  @DataProvider
  public Object[][] scopes() {
    return new Object[][] {{null}, {UUID.randomUUID()}};
  }

  @Test(dataProvider = "scopes")
  public void concurrentConsumptionSucceedsOnlyOnceEvenWithDuplicateCodes(UUID userId) throws Exception {
    try (var database = database()) {
      addCode(database, userId, CODE, NOW + 1000);
      addCode(database, userId, CODE, NOW + 1000);
      var factory = database.factory();
      var workers = Executors.newFixedThreadPool(2);
      var start = new CountDownLatch(1);
      try {
        java.util.concurrent.Callable<Integer> consume = () -> {
          start.await();
          try (var session = factory.openSession(true)) {
            return session.getMapper(EmailVerificationDao.class).consumeCode(EMAIL, userId, CODE, NOW);
          }
        };
        var first = workers.submit(consume);
        var second = workers.submit(consume);
        start.countDown();
        assertThat(java.util.List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS)))
            .containsExactlyInAnyOrder(0, 2);
        assertThat(database.mapper(EmailVerificationDao.class).consumeCode(EMAIL, userId, CODE, NOW)).isZero();
      } finally {
        workers.shutdownNow();
      }
    }
  }

  @Test(dataProvider = "scopes")
  public void checksCodeEmailScopeExpirationAndDoesNotInvalidateOtherCodes(UUID userId) throws Exception {
    try (var database = database()) {
      addCode(database, userId, CODE, NOW);
      addCode(database, userId, "expired", NOW - 1);
      addCode(database, userId, "different", NOW + 1);
      var dao = database.mapper(EmailVerificationDao.class);
      assertThat(dao.consumeCode("other@example.test", userId, CODE, NOW)).isZero();
      assertThat(dao.consumeCode(EMAIL, userId == null ? UUID.randomUUID() : null, CODE, NOW)).isZero();
      assertThat(dao.consumeCode(EMAIL, userId, "wrong", NOW)).isZero();
      assertThat(dao.consumeCode(EMAIL, userId, "expired", NOW)).isZero();
      assertThat(dao.hasExpiredCode(EMAIL, userId, "expired", NOW)).isTrue();
      assertThat(dao.hasExpiredCode(EMAIL, userId, "wrong", NOW)).isFalse();
      assertThat(dao.consumeCode(EMAIL, userId, CODE, NOW)).isEqualTo(1);
      assertThat(dao.consumeCode(EMAIL, userId, "different", NOW)).isEqualTo(1);
    }
  }

  @DataProvider
  public Object[][] businessFlows() {
    return new Object[][] {{true, true}, {true, false}, {false, true}, {false, false}};
  }

  @Test(dataProvider = "businessFlows")
  public void consumptionCommitsWithBusinessWriteAndRollsBackOnFailure(boolean passwordReset, boolean failWrite)
      throws Exception {
    try (var database = database()) {
      var userId = UUID.randomUUID();
      UUID codeScope = passwordReset ? null : userId;
      addCode(database, codeScope, CODE, NOW + 1000);
      var userDao = mock(UserDao.class);
      var user = UserEntityBuilder.newBuilder().setId(userId).setUsername(EMAIL).build();
      when(userDao.getByUsername(EMAIL)).thenReturn(Optional.of(user));
      when(userDao.findById(userId)).thenReturn(Optional.of(user));
      var encoder = mock(PasswordEncoder.class);
      when(encoder.encode("new-password")).thenReturn("encrypted-password");
      if (failWrite) {
        doThrow(new IllegalStateException("write failed")).when(userDao).update(any());
      }
      var dao = database.mapper(EmailVerificationDao.class);
      var service = database.transactional(new UserService(userDao, null, null, null, null, encoder, null,
          null, null, null, null, dao, Clock.fixed(Instant.ofEpochMilli(NOW), ZoneOffset.UTC)), UserService.class);
      try (var helper = mockStatic(UserHelper.class); var constants = mockStatic(UserFacingConstants.class)) {
        helper.when(UserHelper::getCurrentUserId).thenReturn(userId);
        constants.when(() -> UserFacingConstants.getConstant(anyString())).thenReturn(new UserFacingConstants.Constant());
        Runnable action = passwordReset
            ? () -> assertThat(service.updatePassword(new UpdatePasswordRequest().username(EMAIL)
                .password("new-password").verificationCode(CODE)).getResult()).isTrue()
            : () -> assertThat(service.verifyUserEmail(new VerifyByEmailCodeRequest().userId(userId)
                .email(EMAIL).verificationCode(CODE)).getVerifyResult()).isTrue();
        if (failWrite) {
          assertThatThrownBy(action::run).isInstanceOf(IllegalStateException.class).hasMessage("write failed");
        } else {
          action.run();
        }
        assertThat(dao.consumeCode(EMAIL, codeScope, CODE, NOW)).isEqualTo(failWrite ? 1 : 0);
      }
    }
  }

  private ConcurrencyDatabase database() throws Exception {
    var database = new ConcurrencyDatabase();
    database.execute("CREATE TABLE email_verification (id BINARY(16) PRIMARY KEY, user_id BINARY(16), "
        + "email VARCHAR(255), email_verification_code VARCHAR(255), is_used BOOLEAN, "
        + "creation_timestamp BIGINT, expiration_timestamp BIGINT)");
    database.configuration.addMapper(EmailVerificationDao.class);
    return database;
  }

  private void addCode(ConcurrencyDatabase database, UUID userId, String code, long expires) throws Exception {
    try (var statement = database.connection.prepareStatement("INSERT INTO email_verification VALUES (?, ?, ?, ?, FALSE, 0, ?)")) {
      statement.setBytes(1, ConcurrencyDatabase.bytes(UUID.randomUUID()));
      statement.setBytes(2, ConcurrencyDatabase.bytes(userId));
      statement.setString(3, EMAIL);
      statement.setString(4, code);
      statement.setLong(5, expires);
      statement.executeUpdate();
    }
  }
}
