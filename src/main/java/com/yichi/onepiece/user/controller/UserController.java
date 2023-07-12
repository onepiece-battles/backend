package com.yichi.onepiece.user.controller;

import static com.yichi.onepiece.constants.ConstantsContainer.SUCCESS;

import com.yichi.onepiece.api.UserApi;
import com.yichi.onepiece.iam.IamService;
import com.yichi.onepiece.iam.PermissionDefinition.PermissionEnum;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.model.UserEntity;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@Controller
public class UserController implements UserApi {
  private final UserService userService;
  private final IamService iamService;
  private final AuthenticationManager authenticationManager;

  public UserController(
      UserService userService, IamService iamService, AuthenticationManager authenticationManager) {
    this.userService = userService;
    this.iamService = iamService;
    this.authenticationManager = authenticationManager;
  }

  @Override
  public ResponseEntity<AssignRoleResponse> assignRole(AssignRoleRequest assignRoleRequest) {
    iamService.checkPermissionOfCurrentAccount(
        UserEntity.ENTITY_TYPE, Set.of(PermissionEnum.USER_ASSIGN_ROLE));
    var roles =
        userService.assignRoleToUser(assignRoleRequest.getUserId(), assignRoleRequest.getRoles());
    return ResponseEntity.ok(
        new AssignRoleResponse().roles(roles).userId(assignRoleRequest.getUserId()));
  }

  @Override
  public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
    var userName = loginRequest.getUserName();
    var password = loginRequest.getPassword();
    // Authenticate username and password with registered information.
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userName, password));
    var loginResponse = userService.login(userName);
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
            .verification(
                registerSuccess
                    ? null
                    : UserFacingConstants.getConstant(verificationKey).getDisplayName()));
  }
}
