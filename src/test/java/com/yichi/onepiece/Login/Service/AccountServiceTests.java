package com.yichi.onepiece.Login.Service;

import com.yichi.onepiece.Account.Model.AccountDao;
import com.yichi.onepiece.Account.Service.AccountService;
import com.yichi.onepiece.BaseServiceTests;

import static com.yichi.onepiece.Account.Service.AccountService.NO_USER_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import com.yichi.onepiece.model.LogoutResponse;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;

public class AccountServiceTests extends BaseServiceTests {
    private AccountService accountService;

    @Mock
    private AccountDao accountDao;

    @BeforeMethod
    public void setup() {
        accountService = new AccountService(accountDao);
    }

    @Test
    public void testLogOutNoUserFound() {
        UUID userId = UUID.randomUUID();

        when(accountDao.findById(userId)).thenReturn(Optional.empty());

        var result = accountService.logout(userId);
        var expectResult = new LogoutResponse().logoutResult(NO_USER_FOUND);

        assertThat(result).isEqualTo(expectResult);
    }
}
