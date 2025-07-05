package com.yichi.onepiece.user.controller;

import static com.yichi.onepiece.constants.OnepieceConstants.SUCCESS;
import com.yichi.onepiece.api.UserApi;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.service.UserAccountService;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class UserController implements UserApi {
  private final Logger logger = LoggerFactory.getLogger(UserController.class);
  private final UserService userService;
  private final UserAccountService userAccountService;
  private final IamService iamService;

  public UserController(UserService userService, UserAccountService userAccountService, IamService iamService) {
    this.userService = userService;
      this.userAccountService = userAccountService;
      this.iamService = iamService;
  }

  @Override
  public ResponseEntity<AcceptFriendResponse> acceptFriend(AcceptFriendRequest acceptFriendRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.USER, Set.of(Permission.UPDATE));
    var response = userService.acceptFriend(acceptFriendRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AddFriendResponse> addFriend(AddFriendRequest addFriendRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.USER, Set.of(Permission.CREATE));
    var response = userService.addFriend(addFriendRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AssignRoleResponse> assignRole(AssignRoleRequest assignRoleRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.USER, Set.of(Permission.UPDATE));
    var roles =
        userService.assignRoleToUser(assignRoleRequest.getUserId(), assignRoleRequest.getRoles());
    return ResponseEntity.ok(
        new AssignRoleResponse().roles(roles).userId(assignRoleRequest.getUserId()));
  }

  @Override
  public ResponseEntity<User> getUser(GetUserRequest getUserRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.USER, Set.of(Permission.GET));
    var user = userService.getUserById(getUserRequest.getUserId());
    return ResponseEntity.ok(user);
  }

  @Override
  public ResponseEntity<ListUsersResponse> listUsers(ListUsersRequest listUsersRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.USER, Set.of(Permission.LIST));
    var response = userService.list(listUsersRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
    var loginResponse = userAccountService.login(loginRequest);
    return ResponseEntity.ok(loginResponse);
  }

  @Override
  public ResponseEntity<LogoutResponse> logout(LogoutRequest logoutRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.USER, Set.of(Permission.UPDATE));
    var response = userAccountService.logout(logoutRequest.getUserId());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<RegisterResponse> register(RegisterRequest registerRequest) {
    var verificationKey =
            userAccountService.register(
            registerRequest.getNickname(),
            registerRequest.getPassword(),
            registerRequest.getUsername(),
            registerRequest.getConfirmedPassword(),
            registerRequest.getLocale(),
            registerRequest.getUserRole());
    var registerSuccess = verificationKey.equals(SUCCESS);
    return ResponseEntity.ok(
        new RegisterResponse()
            .registerResult(registerSuccess)
            .verification(
                registerSuccess
                    ? null
                    : UserFacingConstants.getConstant(verificationKey).getDisplayName()));
  }

  @Override
  public ResponseEntity<ShareResourceResponse> shareResource(
      ShareResourceRequest shareResourceRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.USER, Set.of(Permission.UPDATE));
    var response = userService.shareResource(shareResourceRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<UpdatePasswordResponse> updatePassword(UpdatePasswordRequest updatePasswordRequest) {
    var response = userService.updatePassword(updatePasswordRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<UpdateLoginRoleResponse> updateRole(UpdateLoginRoleRequest request) {
    var response = userService.updateLoginRole(request.getRole());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<UpdateUserResponse> updateUser(UpdateUserRequest updateUserRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.USER, Set.of(Permission.UPDATE));
    var user = userService.updateUser(updateUserRequest);
    return ResponseEntity.ok(new UpdateUserResponse().user(user));
  }

  @Override
  public ResponseEntity<VerifyByEmailCodeResponse> verifyByEmailCode(
      VerifyByEmailCodeRequest verifyByEmailCodeRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.USER, Set.of(Permission.UPDATE));
    var response = userService.verifyUserEmail(verifyByEmailCodeRequest);
    return ResponseEntity.ok(response);
  }
}
