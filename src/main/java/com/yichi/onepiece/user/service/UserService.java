package com.yichi.onepiece.user.service;

import static com.yichi.onepiece.constants.ConstantsContainer.SUCCESS;
import static com.yichi.onepiece.constants.ConstantsContainer.VALID_EMAIL_PATTERN;
import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.*;

import com.yichi.onepiece.constants.ConstantsContainer;
import com.yichi.onepiece.email.EmailClient;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.iam.model.*;
import com.yichi.onepiece.iam.service.RoleService;
import com.yichi.onepiece.image.service.ImageUtil;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.model.UserRole;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.*;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;
import com.yichi.onepiece.utils.JwtHelper;
import com.yichi.onepiece.utils.RandomGenerator;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final Logger logger = LoggerFactory.getLogger(UserService.class);
  private final UserDao userDao;
  private final RoleToResourcePermissionDao roleToResourcePermissionDao;
  private final ResourcePermissionDao resourcePermissionDao;
  private final AuthenticationManager authenticationManager;
  private final TokenDao tokenDao;
  private final RoleDao roleDao;
  private final UserRoleDao userRoleDao;
  private final PasswordEncoder passwordEncoder;
  private final UserResourceDao userResourceDao;
  private final ActionRecordService actionRecordService;
  private final EmailClient emailClient;
  private final RoleService roleService;

  private final EmailVerificationDao emailVerificationDao;
  private final Clock clock;

  public UserService(
      UserDao userDao,
      RoleToResourcePermissionDao roleToResourcePermissionDao,
      ResourcePermissionDao resourcePermissionDao,
      AuthenticationManager authenticationManager,
      TokenDao tokenDao,
      RoleDao roleDao,
      UserRoleDao userRoleDao,
      PasswordEncoder passwordEncoder,
      UserResourceDao userResourceDao,
      ActionRecordService actionRecordService,
      EmailClient emailClient,
      RoleService roleService,
      EmailVerificationDao emailVerificationDao,
      Clock clock) {
    this.userDao = userDao;
    this.roleToResourcePermissionDao = roleToResourcePermissionDao;
    this.resourcePermissionDao = resourcePermissionDao;
    this.authenticationManager = authenticationManager;
    this.tokenDao = tokenDao;
    this.roleDao = roleDao;
    this.userRoleDao = userRoleDao;
    this.passwordEncoder = passwordEncoder;
    this.userResourceDao = userResourceDao;
    this.actionRecordService = actionRecordService;
    this.emailClient = emailClient;
    this.roleService = roleService;
    this.emailVerificationDao = emailVerificationDao;
    this.clock = clock;
  }

  /**
   * Request to register into system
   *
   * @param nickName nickname
   * @param password password
   * @param username email address as username
   * @param confirmedPassword confirmed password
   * @param onepieceLocale the local information (i18n information) based on FE language
   * @return register verification: key of DUPLICATE_USER_NAME/PASSWORD_DIFFERENT/REGISTER_SUCCESS
   */
  public String register(
      String nickName,
      String password,
      String username,
      String confirmedPassword,
      OnepieceLocale onepieceLocale) {
    if (!userDao.listByName(nickName).isEmpty()) {
      return UserFacingConstantsKey.DUPLICATE_USER_NAME;
    }
    if (!userDao.listByEmailAddress(username).isEmpty()) {
      return UserFacingConstantsKey.EMAIL_HAS_BEEN_REGISTERED;
    }
    if (!username.matches(VALID_EMAIL_PATTERN)) {
      return UserFacingConstantsKey.INVALID_EMAIL_ADDRESS;
    }
    if (!password.equals(confirmedPassword)) {
      return UserFacingConstantsKey.PASSWORD_CONFIRMATION;
    }
    var defaultImage =
        ImageUtil.loadImageFromDisk(ConstantsContainer.USER_DEFAULT_PROFILE_PATH, "PNG");
    var encryptedPassword = passwordEncoder.encode(password);
    UserEntity user =
        UserEntityBuilder.newBuilder()
            .setNickname(nickName)
            .setAccountStatus(AccountStatus.LOGGED_OUT)
            .setEncryptedPassword(encryptedPassword)
            .setUsername(username)
            .setEmailVerified(false)
            .setLocale(onepieceLocale)
            // USER role is the default login role
            .setLoginRole(UserRole.NIKA)
            .setProfileImage(ArrayUtils.toObject(defaultImage))
            .build();
    userDao.insert(user);
    var actionRecord = actionRecordService.createWithUser(user);
    var updatedUser =
        user.toBuilder()
            .setCreationRecordId(actionRecord.getId())
            .setModificationRecordId(actionRecord.getId())
            .build();
    userDao.update(updatedUser);
    // newly registered users are default to have role: "NIKA", "NORMAL", "ADMIN" and "VISITOR"
    // will be changed in the future
    var roleEntities = roleDao.listRoleByRoleNames(Arrays.stream(UserRole.values()).toList());
    roleEntities.forEach(
        roleEntity ->
            userRoleDao.insert(
                UserRoleEntityBuilder.newBuilder()
                    .setUserId(user.getId())
                    .setRoleId(roleEntity.getId())
                    .build()));
    return SUCCESS;
  }

  public LoginResponse login(LoginRequest request) {
    var email = request.getEmail();
    var password = request.getPassword();
    var userDetail = userDao.getUserByUsername(email);
    I18nText verification;
    if (userDetail.isEmpty()) {
      verification = UserFacingConstants.getConstant(REGISTER_FIRST_BEFORE_LOGIN).getDisplayName();
      return new LoginResponse().verification(verification);
    }
    // Authenticate username and password with registered information.
    try {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
    } catch (Exception e) {
      verification = UserFacingConstants.getConstant(WRONG_PASSWORD).getDisplayName();
      return new LoginResponse().verification(verification);
    }

    // generate jwt token and refresh token for this login action
    var user = userDetail.get();
    var jwtToken = JwtHelper.generateAccessToken(user);
    var jwtRefreshToken = JwtHelper.generateRefreshToken(user);

    // revoke all user tokens
    tokenDao.revokeAllUserTokensByUserId(user.getId());
    // add new token
    TokenEntity tokenEntity =
        TokenEntityBuilder.newBuilder()
            .setUserId(user.getId())
            .setTokenType(TokenType.BEARER)
            .setTokenValue(jwtToken)
            .setExpired(false)
            .setRevoked(false)
            .build();
    tokenDao.insert(tokenEntity);
    user.setAccountStatus(AccountStatus.LOGGED_IN);
    userDao.update(user);
    var resourcePermissionMaps = getPermissionMapByRoleName(user.getLoginRole());
    var tokens =
        tokenDao.listTokenByUserId(user.getId()).stream().map(TokenEntity::getTokenValue).toList();
    var roleIds = userRoleDao.getByUserId(user.getId());
    var roleNames =
        roleDao.batchFindById(roleIds).stream()
            .map(RoleEntity::getName)
            .collect(Collectors.toList());
    return new LoginResponse()
        .verification(null)
        .accessToken(jwtToken)
        .refreshToken(jwtRefreshToken)
        .user(
            UserConverter.INSTANCE
                .toUser(user)
                .loginRoleResourcesPermissionMap(resourcePermissionMaps)
                .userRoles(roleNames)
                .tokens(tokens));
  }

  private List<ResourcePermissionMap> getPermissionMapByRoleName(UserRole userRole) {
    var roleId = roleDao.findByRoleName(userRole).orElseThrow().getId();
    return roleService.getResourcePermissionMapByRoleId(roleId).entrySet().stream()
        .map(
            entry -> {
              var resourcePermissionMap = new ResourcePermissionMap();
              resourcePermissionMap.setResourceType(entry.getKey());
              resourcePermissionMap.setPermissions(entry.getValue());
              return resourcePermissionMap;
            })
        .toList();
  }

  public LogoutResponse logout(UUID userId) {
    var userOptional = userDao.findById(userId);
    if (userOptional.isEmpty()) {
      return new LogoutResponse().logoutResult(NO_USER_FOUND);
    }

    var user = userOptional.get();
    var signedOutUser = user.toBuilder().setAccountStatus(AccountStatus.LOGGED_OUT).build();
    userDao.update(signedOutUser);
    return new LogoutResponse().logoutResult(SUCCESS).userName(user.getUsername());
  }

  public ListUsersResponse list(ListUsersRequest request) {
    var totalSize = userDao.searchCount(request.getUserFilter());
    // pageIndex is 1- based. offset is 0- based.
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return new ListUsersResponse().users(List.of()).totalSize(totalSize);
    }
    var users =
        userDao
            .listUsers(
                request.getUserFilter(), request.getSortKeys(), offset, request.getPageSize())
            .stream()
            .map(UserConverter.INSTANCE::toUser)
            .toList();
    return new ListUsersResponse().users(users).totalSize(totalSize);
  }

  public String updateProfile(UUID userId, String profile) {
    Byte[] profileByte = ArrayUtils.toObject(EncryptHelper.base64Decode(profile));
    var user =
        userDao
            .findById(userId)
            .orElseThrow(ExceptionUtils.notFound("user with id %s not found", userId));
    var updatedUser = user.toBuilder().setProfileImage(profileByte).build();
    userDao.update(updatedUser);
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(updatedUser.getProfileImage()));
  }

  public String getProfile(UUID userId) {
    var user =
        userDao
            .findById(userId)
            .orElseThrow(ExceptionUtils.notFound("user with id %s not found", userId));
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(user.getProfileImage()));
  }

  public List<UserRole> assignRoleToUser(UUID userId, List<UserRole> roles) {
    var roleEntities = roleDao.listAllRoles();
    var roleNameFullSet =
        roleEntities.stream()
            .map(RoleEntity::getName)
            .collect(Collectors.toCollection(HashSet::new));
    var roleIds = roleEntities.stream().map(RoleEntity::getId).collect(Collectors.toSet());
    if (roleNameFullSet.containsAll(roles)) {
      List<UserRoleEntity> userRoleEntities = new ArrayList<>();
      for (var roleId : roleIds) {
        var userRoleEntity =
            UserRoleEntityBuilder.newBuilder().setUserId(userId).setRoleId(roleId).build();
        userRoleEntities.add(userRoleEntity);
      }
      userRoleDao.batchInsert(userRoleEntities);
      return roles;
    } else {
      throw ExceptionUtils.notFound(
              "Role name not found in db when assigning role to user %s", userId)
          .get();
    }
  }

  public List<ResourcePermissionMap> updateLoginRole(UserRole role) {
    var user = UserHelper.getCurrentUserDetail();
    if (!user.getUserRoles().contains(role.toString())) {
      ExceptionUtils.notFound("user role %s not found when shifting role.", role).get();
    }
    user.setLoginRole(role);
    userDao.update(user);
    return getPermissionMapByRoleName(role);
  }

  public User getUserById(UUID userId) {
    var user = userDao.getById(userId).orElseThrow();
    return UserConverter.INSTANCE.toUser(user);
  }

  public ShareResourceResponse shareResource(ShareResourceRequest request) {
    UUID sourceUserId = request.getSourceUserId();
    UUID targetUserId = request.getTargetUserId();
    var userResourceIds =
        userResourceDao.getResourceIdsByUserIdAndResourceType(
            targetUserId, request.getResourceType());
    userResourceIds.addAll(request.getResourceIds());
    var userResourceEntities =
        userResourceIds.stream()
            .map(
                id ->
                    UserResourceEntityBuilder.newBuilder()
                        .setUserId(targetUserId)
                        .setSourceUserId(sourceUserId)
                        .setResourceType(request.getResourceType())
                        .setResourceId(id)
                        .build())
            .toList();
    userResourceDao.batchInsert(userResourceEntities);
    ShareResourceResponse response = new ShareResourceResponse();
    response.setSharingInformation(
        UserFacingConstants.getConstant(RESOURCE_SHARING_SUCCESS).getDisplayName());
    return response;
  }

  public List<UUID> getResourceIdsByUserIdResourceType(UUID userId, ResourceType resourceType) {
    return userResourceDao.getResourceIdsByUserIdAndResourceType(userId, resourceType);
  }

  public User updateUser(UpdateUserRequest request) {
    var user = userDao.findById(request.getId()).orElseThrow();
    var updatedUser =
        user.toBuilder().setLocale(request.getLocale()).setNickname(request.getNickname()).build();
    userDao.update(updatedUser);
    var loginUserResource = getPermissionMapByRoleName(user.getLoginRole());
    var tokens =
        tokenDao.listTokenByUserId(user.getId()).stream().map(TokenEntity::getTokenValue).toList();
    var roleIds = userRoleDao.getByUserId(user.getId());
    var roleNames = roleDao.batchFindById(roleIds).stream().map(RoleEntity::getName).toList();
    return UserConverter.INSTANCE
        .toUser(userDao.getById(request.getId()).orElseThrow())
        .loginRoleResourcesPermissionMap(loginUserResource)
        .userRoles(roleNames)
        .tokens(tokens);
  }

  public I18nText verifyEmail(VerifyEmailRequest verifyEmailRequest) {
    // todo(yichi): send email according to locale information.
    try {
      long creationTimestamp = clock.millis();
      // 5 minutes later
      long expirationTimestamp = creationTimestamp + 300000L;
      String verificationCode = RandomGenerator.generateVerificationCode();
      var emailVerificationEntity =
          EmailVerificationEntityBuilder.newBuilder()
              .setEmail(verifyEmailRequest.getEmail())
              .setUserId(verifyEmailRequest.getId())
              .setEmailVerificationCode(verificationCode)
              .setCreationTimestamp(clock.millis())
              .setExpirationTimestamp(expirationTimestamp)
              .isUsed(false)
              .build();
      emailVerificationDao.insert(emailVerificationEntity);
      emailClient.sendMail(
          verifyEmailRequest.getEmail(),
          "This is the verification email, The verification code is: " + verificationCode,
          "Email address verification");
    } catch (Exception e) {
      return UserFacingConstants.getConstant(EMAIL_SENDING_FAILED).getDisplayName();
    }
    return UserFacingConstants.getConstant(EMAIL_SENT).getDisplayName();
  }

  public VerifyByEmailCodeResponse verifyByCode(VerifyByEmailCodeRequest request) {
    var emailVerificationEntity =
        emailVerificationDao
            .getByEmailAndUserId(request.getEmail(), request.getUserId())
            .orElseThrow();
    boolean verifyResult =
        emailVerificationEntity.getEmailVerificationCode().equals(request.getVerificationCode())
            && !emailVerificationEntity.getIsUsed()
            && emailVerificationEntity.getExpirationTimestamp() > clock.millis();
    // if verification code is correct, send mark user email as verified.
    if (verifyResult) {
      var user = userDao.findById(request.getUserId()).orElseThrow();
      user.toBuilder().setEmailVerified(true);
      userDao.update(user);
    }
    return new VerifyByEmailCodeResponse().verifyResult(verifyResult);
  }
}
