package com.yichi.onepiece.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.iam.model.TokenEntity;
import com.yichi.onepiece.model.UserFilter;
import com.yichi.onepiece.model.UserRole;
import java.sql.DriverManager;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.testng.annotations.Test;

public class UserListPrivacyTests {
  @Test
  public void listQueryWorksAfterProfileImageColumnRemoved() throws Exception {
    var configuration = new Configuration();
    configuration.getTypeHandlerRegistry().register("com.yichi.infra.support.mybatis.typehandler");
    String resource = "com/yichi/onepiece/user/model/userDao.xml";
    try (var input = Resources.getResourceAsStream(resource)) {
      new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
    }
    // Match the user projection after V28: avatars live in image_uri and the
    // old profile_image column does not exist.
    try (var connection = DriverManager.getConnection(
        "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        var statement = connection.createStatement()) {
      statement.execute("CREATE TABLE `user` (id VARCHAR(36), nickname VARCHAR(255), "
          + "username VARCHAR(255), login_role VARCHAR(32), encrypted_password VARCHAR(255), "
          + "account_status VARCHAR(32), locale VARCHAR(32), email_verified BOOLEAN, "
          + "creation_record_id BIGINT, modification_record_id BIGINT, image_uri VARCHAR(1024))");
      statement.execute("INSERT INTO `user` (id, nickname, username, login_role, creation_record_id, image_uri) "
          + "VALUES ('nika-id', 'Nika', 'nika@example.test', 'NIKA', 1, 'https://images.example.test/nika.png')");
      var parameters = Map.of(
          "userFilter", new UserFilter().userRoles(List.of(UserRole.NIKA)),
          "sortList", List.of(), "offset", 0, "size", 10);
      var query = configuration.getMappedStatement(UserDao.class.getName() + ".listUsers");
      var sql = query.getBoundSql(parameters);
      try (var prepared = connection.prepareStatement(sql.getSql())) {
        new DefaultParameterHandler(query, parameters, sql).setParameters(prepared);
        try (var rows = prepared.executeQuery()) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString("id")).isEqualTo("nika-id");
          assertThat(rows.getString("image_uri")).isEqualTo("https://images.example.test/nika.png");
          assertThat(rows.next()).isFalse();
        }
      }
    }
  }

  @Test
  public void listedUserDoesNotExposeTokensEvenIfSourceContainsThem() {
    var user = new UserDetailImpl();
    user.setId(UUID.randomUUID());
    user.setNickname("test-user");
    var token = mock(TokenEntity.class);
    when(token.getTokenValue()).thenReturn("private-token");
    user.setTokens(List.of(token));
    var listedUser = UserConverter.INSTANCE.toUser(user);
    assertThat(listedUser.getId()).isEqualTo(user.getId());
    assertThat(listedUser.getNickname()).isEqualTo("test-user");
    assertThat(listedUser.getTokens()).isNullOrEmpty();
    verifyNoInteractions(token);
  }

  @Test
  public void listResultMapDoesNotLoadTokensWhileDetailMapStillDoes() throws Exception {
    var configuration = new Configuration();
    configuration.getTypeHandlerRegistry().register("com.yichi.infra.support.mybatis.typehandler");
    String resource = "com/yichi/onepiece/user/model/userDao.xml";
    try (var input = Resources.getResourceAsStream(resource)) {
      new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
    }
    var listMap = configuration.getResultMap(UserDao.class.getName() + ".userListView");
    var detailMap = configuration.getResultMap(UserDao.class.getName() + ".userDetailView");
    assertThat(listMap.getResultMappings()).noneMatch(mapping -> "tokens".equals(mapping.getProperty()));
    assertThat(detailMap.getResultMappings()).anyMatch(mapping -> "tokens".equals(mapping.getProperty()));
  }
}
