package com.yichi.onepiece.Login.Service;

import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.NO_USER_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yichi.onepiece.BaseServiceTests;
import com.yichi.onepiece.iam.model.RoleDao;
import com.yichi.onepiece.iam.model.TokenDao;
import com.yichi.onepiece.iam.model.UserRoleDao;
import com.yichi.onepiece.model.LogoutResponse;
import com.yichi.onepiece.user.model.UserDao;
import com.yichi.onepiece.user.model.UserDetailDao;
import com.yichi.onepiece.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UserServiceTests extends BaseServiceTests {
  private UserService userService;
  @Mock private UserDao userDao;
  @Mock private TokenDao tokenDao;
  @Mock private RoleDao roleDao;
  @Mock private UserRoleDao userRoleDao;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private UserDetailDao userDetailDao;

  @BeforeMethod
  public void setup() {
    userService =
        new UserService(userDao, userDetailDao, tokenDao, roleDao, userRoleDao, passwordEncoder);
  }

  @Test
  public void testLogOutNoUserFound() {
    UUID userId = UUID.randomUUID();

    when(userDao.findById(userId)).thenReturn(Optional.empty());

    var result = userService.logout(userId);
    var expectResult = new LogoutResponse().logoutResult(NO_USER_FOUND);

    assertThat(result).isEqualTo(expectResult);
  }
}
