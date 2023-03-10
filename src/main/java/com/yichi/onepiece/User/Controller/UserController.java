package com.yichi.onepiece.User.Controller;

import com.yichi.onepiece.UFC.UserFacingConstants;
import com.yichi.onepiece.User.Service.UserService;
import com.yichi.onepiece.api.UserApi;
import com.yichi.onepiece.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import static com.yichi.onepiece.Constants.ConstantsContainer.SUCCESS;

@Controller
public class UserController implements UserApi {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        var userName = loginRequest.getUserName();
        var password = loginRequest.getPassword();
        var email = loginRequest.getEmail();
        var org = loginRequest.getOrganization();
        var loginResponse = userService.login(userName, password, email, org);
        return ResponseEntity.ok(loginResponse);
    }

    @Override
    public ResponseEntity<LogoutResponse> logout(LogoutRequest logoutRequest) {
        var response = userService.logout(logoutRequest.getUserId());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<RegisterResponse> register(RegisterRequest registerRequest) {
        var verificationKey =
                userService.register(
                        registerRequest.getUserName(),
                        registerRequest.getPassword(),
                        registerRequest.getEmail(),
                        registerRequest.getConfirmedPassword(),
                        registerRequest.getOrganization());
        var registerSuccess = verificationKey.equals(SUCCESS);
        return ResponseEntity.ok(
                new RegisterResponse()
                        .registerResult(registerSuccess)
                        .verification(registerSuccess ? null : UserFacingConstants.getConstant(verificationKey).getDisplayName()));
    }
}
