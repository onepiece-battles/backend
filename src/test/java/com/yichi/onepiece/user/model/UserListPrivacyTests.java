package com.yichi.onepiece.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.iam.model.TokenEntity;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.testng.annotations.Test;

public class UserListPrivacyTests {
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
