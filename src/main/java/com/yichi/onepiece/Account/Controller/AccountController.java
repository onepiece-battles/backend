package com.yichi.onepiece.Account.Controller;

import com.yichi.onepiece.Account.Service.AccountService;
import com.yichi.onepiece.api.AccountApi;
import com.yichi.onepiece.model.LoginRequest;
import com.yichi.onepiece.model.LoginResponse;
import com.yichi.onepiece.model.LogoutRequest;
import com.yichi.onepiece.model.LogoutResponse;
import com.yichi.onepiece.model.RegisterRequest;
import com.yichi.onepiece.model.RegisterResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class AccountController implements AccountApi {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
    var userName = loginRequest.getUserName();
    var password = loginRequest.getPassword();
    var email = loginRequest.getEmail();
    var org = loginRequest.getOrganization();
    var loginResponse = accountService.login(userName, password,email, org);
    return ResponseEntity.ok(loginResponse);
  }

  @Override
  public ResponseEntity<LogoutResponse> logout(LogoutRequest logoutRequest) {
    var response = accountService.logout(logoutRequest.getUserId());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<RegisterResponse> register(RegisterRequest registerRequest) {
    var verification =
        accountService.register(
            registerRequest.getUserName(),
            registerRequest.getPassword(),
            registerRequest.getEmail(),
            registerRequest.getConfirmedPassword(),
            registerRequest.getOrganization());
    var registerResult = verification.equals(AccountService.SUCCESS);
    return ResponseEntity.ok(
        new RegisterResponse().registerResult(registerResult).verification(verification));
  }
}
