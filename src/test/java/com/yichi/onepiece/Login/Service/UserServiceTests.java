package com.yichi.onepiece.Login.Service;

import com.yichi.onepiece.User.Model.UserDao;
import com.yichi.onepiece.User.Service.UserService;
import com.yichi.onepiece.BaseServiceTests;

import static com.yichi.onepiece.UFC.UserFacingConstantsKey.NO_USER_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import com.yichi.onepiece.model.LogoutResponse;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;

public class UserServiceTests extends BaseServiceTests {
    private UserService userService;

    @Mock
    private UserDao userDao;

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
