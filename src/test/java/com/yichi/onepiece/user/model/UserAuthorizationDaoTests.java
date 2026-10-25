package com.yichi.onepiece.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.model.AcceptFriendRequest;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.notification.service.NotificationService;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class UserAuthorizationDaoTests {
  @DataProvider
  public Object[][] shareableTypes() {
    return new Object[][] {{ResourceType.CHARACTER}, {ResourceType.FRUIT},
        {ResourceType.BATTLE}, {ResourceType.GROUP}};
  }

  @Test(dataProvider = "shareableTypes")
  public void checksOriginalOwnerAndSystemFlagForEveryShareableType(ResourceType resourceType) throws Exception {
    try (var database = new Database()) {
      var ownerId = UUID.randomUUID();
      var foreignOwnerId = UUID.randomUUID();
      database.addCreation(1, ownerId);
      database.addCreation(2, foreignOwnerId);
      var ownedId = UUID.randomUUID();
      var foreignId = UUID.randomUUID();
      var systemId = UUID.randomUUID();
      String table = resourceType.name().toLowerCase(java.util.Locale.ROOT);
      database.addResource(table, ownedId, 1, false);
      database.addResource(table, foreignId, 2, false);
      database.addResource(table, systemId, 1, true);
      try (var session = database.factory.openSession(true)) {
        var dao = session.getMapper(UserResourceDao.class);
        var requestedIds = List.of(ownedId, foreignId, systemId, UUID.randomUUID());
        assertThat(dao.countShareableResources(ownerId, resourceType, requestedIds, false)).isEqualTo(1);
        assertThat(dao.countShareableResources(ownerId, resourceType, requestedIds, true)).isEqualTo(3);
        assertThat(dao.countShareableResources(ownerId, resourceType, List.of(foreignId), false)).isZero();
        assertThat(dao.countShareableResources(ownerId, resourceType, List.of(), true)).isZero();
      }
    }
  }

  @Test
  public void unsupportedTypesNeverAuthorizeEvenForAdmins() throws Exception {
    try (var database = new Database(); var session = database.factory.openSession(true)) {
      var dao = session.getMapper(UserResourceDao.class);
      for (var resourceType : List.of(ResourceType.USER, ResourceType.ROLE, ResourceType.ISSUE, ResourceType.CHATTING)) {
        assertThat(dao.countShareableResources(UUID.randomUUID(), resourceType,
            List.of(UUID.randomUUID()), true)).isZero();
      }
      assertThat(dao.countShareableResources(UUID.randomUUID(), null, List.of(UUID.randomUUID()), true)).isZero();
    }
  }

  @DataProvider
  public Object[][] nonPendingStatuses() {
    return new Object[][] {{"ACCEPTED"}, {"REJECTED"}};
  }

  @Test(dataProvider = "nonPendingStatuses")
  public void cannotAcceptRejectedOrPreviouslyAcceptedRequests(String status) throws Exception {
    try (var database = new Database()) {
      var applicantId = UUID.randomUUID();
      var recipientId = UUID.randomUUID();
      database.addFriendship(applicantId, recipientId, status);
      try (var session = database.factory.openSession(true)) {
        assertThat(session.getMapper(FriendshipDao.class).acceptPendingRequests(applicantId, recipientId)).isZero();
      }
    }
  }

  @Test
  public void rejectsApplicantAndStrangerAndPreventsReplay() throws Exception {
    try (var database = new Database()) {
      var applicantId = UUID.randomUUID();
      var recipientId = UUID.randomUUID();
      database.addFriendship(applicantId, recipientId, "APPLIED");
      try (var session = database.factory.openSession(true)) {
        var dao = session.getMapper(FriendshipDao.class);
        assertThat(dao.acceptPendingRequests(recipientId, applicantId)).isZero();
        assertThat(dao.acceptPendingRequests(applicantId, UUID.randomUUID())).isZero();
        assertThat(dao.acceptPendingRequests(applicantId, recipientId)).isEqualTo(1);
        assertThat(dao.acceptPendingRequests(applicantId, recipientId)).isZero();
      }
    }
  }

  @Test
  public void concurrentAcceptanceUpdatesRequestOnlyOnce() throws Exception {
    try (var database = new Database()) {
      var applicantId = UUID.randomUUID();
      var recipientId = UUID.randomUUID();
      database.addFriendship(applicantId, recipientId, "APPLIED");
      var workers = Executors.newFixedThreadPool(2);
      var start = new CountDownLatch(1);
      try {
        java.util.concurrent.Callable<Integer> accept = () -> {
          start.await();
          try (var session = database.factory.openSession(true)) {
            return session.getMapper(FriendshipDao.class).acceptPendingRequests(applicantId, recipientId);
          }
        };
        var first = workers.submit(accept);
        var second = workers.submit(accept);
        start.countDown();
        assertThat(first.get(5, TimeUnit.SECONDS) + second.get(5, TimeUnit.SECONDS)).isEqualTo(1);
      } finally {
        workers.shutdownNow();
      }
    }
  }

  @Test
  public void rollsBackFriendshipAcceptanceIfNotificationFails() throws Exception {
    try (var database = new Database()) {
      var applicantId = UUID.randomUUID();
      var recipientId = UUID.randomUUID();
      database.addFriendship(applicantId, recipientId, "APPLIED");
      var sessionTemplate = new SqlSessionTemplate(database.factory);
      var userDao = mock(UserDao.class);
      var recipient = mock(UserEntity.class);
      when(recipient.getNickname()).thenReturn("Recipient");
      when(userDao.findById(recipientId)).thenReturn(java.util.Optional.of(recipient));
      var notifications = mock(NotificationService.class);
      when(notifications.create(any())).thenThrow(new IllegalStateException("notification failed"));
      var service = new UserService(userDao, sessionTemplate.getMapper(FriendshipDao.class), null, null,
          null, null, null, null, null, null, notifications, null, null);
      var proxyFactory = new ProxyFactory(service);
      var transactionInterceptor = new TransactionInterceptor();
      transactionInterceptor.setTransactionManager(new DataSourceTransactionManager(database.dataSource));
      transactionInterceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
      proxyFactory.addAdvice(transactionInterceptor);
      var transactionalService = (UserService) proxyFactory.getProxy();
      try (var userHelper = mockStatic(UserHelper.class)) {
        userHelper.when(UserHelper::getCurrentUserId).thenReturn(recipientId);
        assertThatThrownBy(() -> transactionalService.acceptFriend(new AcceptFriendRequest().friendId(applicantId)))
            .isInstanceOf(IllegalStateException.class).hasMessage("notification failed");
      }
      try (var session = database.factory.openSession(true)) {
        assertThat(session.getMapper(FriendshipDao.class).acceptPendingRequests(applicantId, recipientId)).isEqualTo(1);
      }
    }
  }

  private static class Database implements AutoCloseable {
    private final Connection connection;
    private final SqlSessionFactory factory;
    private final UnpooledDataSource dataSource;

    private Database() throws Exception {
      String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL";
      connection = DriverManager.getConnection(url, "sa", "");
      try (var statement = connection.createStatement()) {
        statement.execute("CREATE TABLE action_lineage (id BIGINT PRIMARY KEY, user_id BINARY(16))");
        for (String table : List.of("character", "fruit", "battle", "group")) {
          statement.execute("CREATE TABLE `" + table
              + "` (id BINARY(16) PRIMARY KEY, creation_record_id BIGINT, system_resource BOOLEAN)");
        }
        statement.execute("CREATE TABLE friendship (id BINARY(16) PRIMARY KEY, "
            + "friend_left_id BINARY(16), friend_right_id BINARY(16), friendship_status VARCHAR(20))");
      }
      dataSource = new UnpooledDataSource("org.h2.Driver", url, "sa", "");
      var configuration = new Configuration(new Environment("test", new SpringManagedTransactionFactory(), dataSource));
      configuration.getTypeHandlerRegistry().register("com.yichi.infra.support.mybatis.typehandler");
      configuration.addMapper(UserResourceDao.class);
      configuration.addMapper(FriendshipDao.class);
      factory = new SqlSessionFactoryBuilder().build(configuration);
    }

    private void addCreation(long recordId, UUID ownerId) throws Exception {
      try (var statement = connection.prepareStatement("INSERT INTO action_lineage VALUES (?, ?)")) {
        statement.setLong(1, recordId);
        statement.setBytes(2, uuidBytes(ownerId));
        statement.executeUpdate();
      }
    }

    private void addResource(String table, UUID resourceId, long creationRecordId, boolean system) throws Exception {
      try (var statement = connection.prepareStatement("INSERT INTO `" + table + "` VALUES (?, ?, ?)")) {
        statement.setBytes(1, uuidBytes(resourceId));
        statement.setLong(2, creationRecordId);
        statement.setBoolean(3, system);
        statement.executeUpdate();
      }
    }

    private void addFriendship(UUID applicantId, UUID recipientId, String status) throws Exception {
      try (var statement = connection.prepareStatement("INSERT INTO friendship VALUES (?, ?, ?, ?)")) {
        statement.setBytes(1, uuidBytes(UUID.randomUUID()));
        statement.setBytes(2, uuidBytes(applicantId));
        statement.setBytes(3, uuidBytes(recipientId));
        statement.setString(4, status);
        statement.executeUpdate();
      }
    }

    private byte[] uuidBytes(UUID value) {
      return ByteBuffer.allocate(16).putLong(value.getMostSignificantBits())
          .putLong(value.getLeastSignificantBits()).array();
    }

    @Override
    public void close() throws Exception {
      connection.close();
    }
  }
}
