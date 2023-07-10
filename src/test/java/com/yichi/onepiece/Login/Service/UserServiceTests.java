package com.yichi.onepiece.Login.Service;

import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.NO_USER_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yichi.onepiece.BaseServiceTests;
import com.yichi.onepiece.iam.model.TokenDao;
import com.yichi.onepiece.model.LogoutResponse;
import com.yichi.onepiece.user.Model.UserDao;
import com.yichi.onepiece.user.Service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UserServiceTests extends BaseServiceTests {
  private UserService userService;

  @Mock private UserDao userDao;

  private TokenDao tokenDao;

  @BeforeMethod
  public void setup() {
    userService = new UserService(userDao, tokenDao);
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
