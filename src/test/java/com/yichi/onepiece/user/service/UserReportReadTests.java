package com.yichi.onepiece.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.user.model.UserDao;
import com.yichi.onepiece.user.model.UserEntityBuilder;
import java.util.Optional;
import java.util.UUID;
import org.testng.annotations.Test;

public class UserReportReadTests {
  @Test
  public void reportIdentityReadDoesNotLoadTokenRoleOrLineageAssociations() {
    var dao = mock(UserDao.class);
    var id = UUID.randomUUID();
    var user = UserEntityBuilder.newBuilder().setId(id).setUsername("captain@example.test").build();
    when(dao.findById(id)).thenReturn(Optional.of(user));
    var service = new UserService(dao, null, null, null, null, null, null, null, null, null, null, null, null);
    assertThat(service.getUserEntityById(id)).isSameAs(user);
    verify(dao).findById(id);
    verifyNoMoreInteractions(dao);
  }
}
