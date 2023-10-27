package com.yichi.onepiece.user.service;

import static com.yichi.onepiece.constants.ConstantsContainer.SUCCESS;
import static com.yichi.onepiece.constants.ConstantsContainer.VALID_EMAIL_PATTERN;
import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.*;

import com.yichi.onepiece.constants.ConstantsContainer;
import com.yichi.onepiece.email.EmailClient;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.iam.model.*;
import com.yichi.onepiece.image.service.ImageUtil;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.*;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;
import com.yichi.onepiece.utils.JwtHelper;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final Logger logger = LoggerFactory.getLogger(UserService.class);
  private final UserDao userDao;
  private final UserDetailDao userDetailDao;
  private final TokenDao tokenDao;
  private final RoleResourceDao roleResourceDao;
  private final RoleDao roleDao;
  private final UserRoleDao userRoleDao;
  private final PasswordEncoder passwordEncoder;
  private final UserResourceDao userResourceDao;
  private final EmailClient emailClient;

  private final EmailVerificationDao emailVerificationDao;
  private final Clock clock;

  public UserService(
      UserDao userDao,
      UserDetailDao userDetailDao,
      TokenDao tokenDao,
      RoleResourceDao roleResourceDao,
      RoleDao roleDao,
      UserRoleDao userRoleDao,
      PasswordEncoder passwordEncoder,
      UserResourceDao userResourceDao,
      EmailClient emailClient, EmailVerificationDao emailVerificationDao, Clock clock) {
    this.userDao = userDao;
    this.userDetailDao = userDetailDao;
    this.tokenDao = tokenDao;
    this.roleResourceDao = roleResourceDao;
    this.roleDao = roleDao;
    this.userRoleDao = userRoleDao;
    this.passwordEncoder = passwordEncoder;
    this.userResourceDao = userResourceDao;
    this.emailClient = emailClient;
    this.emailVerificationDao = emailVerificationDao;
    this.clock = clock;
  }

  /**
   * Request to register into system
   *
   * @param name username
   * @param password password
   * @param email email address
   * @param confirmedPassword confirmed password
   * @param organization organization user belongs to
   * @return register verification: key of DUPLICATE_USER_NAME/PASSWORD_DIFFERENT/REGISTER_SUCCESS
   */
  public String register(
      String name,
      String password,
      String email,
      String confirmedPassword,
      String organization,
      OnepieceLocale onepieceLocale) {
    if (!userDao.listByName(name).isEmpty()) {
      return UserFacingConstantsKey.DUPLICATE_USER_NAME;
    }
    if (!userDao.listByEmailAddress(email).isEmpty()) {
      return UserFacingConstantsKey.EMAIL_HAS_BEEN_REGISTERED;
    }
    if (!email.matches(VALID_EMAIL_PATTERN)) {
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
            .setName(name)
            .setAccountCondition(UserAccountCondition.Registered)
            .setEncryptedPassword(encryptedPassword)
            .setEmail(email)
            .setEmailVerified(false)
            .setLocale(onepieceLocale)
            // USER role is the default login role
            .setLoginRole(UserRole.NIKA.toString())
            .setOrganization(organization)
            .setProfileImage(ArrayUtils.toObject(defaultImage))
            .build();
    userDao.insert(user);
    // newly registered users are default to have role: "USER"
    var defaultRoleEntity =
        roleDao
            .findByRoleName(UserRole.USER.toString())
            .orElseThrow(
                ExceptionUtils.notFound("role with name %s not found", UserRole.USER.toString()));
    userRoleDao.insert(
        UserRoleEntityBuilder.newBuilder()
            .setUserId(user.getId())
            .setRoleId(defaultRoleEntity.getId())
            .build());
    return SUCCESS;
  }

  public LoginResponse login(String name) {
    var userDetail = userDetailDao.getUserDetailByName(name);
    I18nText verification;
    if (userDetail.isEmpty()) {
      verification = UserFacingConstants.getConstant(REGISTER_FIRST_BEFORE_LOGIN).getDisplayName();
      return new LoginResponse().verification(verification);
    }
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
    user.setAccountCondition(UserAccountCondition.SignIn);
    userDetailDao.update(user);
    var loginUserResource = roleResourceDao.getResourceNamesByRoleName(user.getLoginRole());
    var tokens =
        tokenDao.listTokenByUserId(user.getId()).stream().map(TokenEntity::getTokenValue).toList();
    return new LoginResponse()
        .verification(null)
        .accessToken(jwtToken)
        .refreshToken(jwtRefreshToken)
        .user(
            UserConverter.INSTANCE
                .toUser(user)
                .loginRoleResources(loginUserResource)
                .tokens(tokens));
  }

  public LogoutResponse logout(UUID userId) {
    var userOptional = userDao.findById(userId);
    if (userOptional.isEmpty()) {
      return new LogoutResponse().logoutResult(NO_USER_FOUND);
    }

    var user = userOptional.get();
    var signedOutUser = user.toBuilder().setAccountCondition(UserAccountCondition.SignOut).build();
    userDao.update(signedOutUser);
    return new LogoutResponse().logoutResult(SUCCESS).userName(user.getName());
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

  public Set<String> assignRoleToUser(UUID userId, Set<String> roles) {
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

  public List<ResourceType> updateLoginRole(String role) {
    var user = UserHelper.getCurrentUserDetail();
    if (!user.getUserRoles().contains(role)) {
      ExceptionUtils.notFound("user role %s not found when shifting role.", role).get();
    }
    user.setLoginRole(role);
    userDao.update(user);
    return roleResourceDao.getResourceNamesByRoleName(role);
  }

  public User getUserById(UUID userId) {
    var user = userDao.findById(userId).orElseThrow();
    var resourceNames = roleResourceDao.getResourceNamesByRoleName(user.getLoginRole());
    return UserConverter.INSTANCE.toUser(user).loginRoleResources(resourceNames);
  }

  public ShareResourceResponse shareResource(ShareResourceRequest request) {
    UUID sourceUserId = request.getSourceUserId();
    UUID targetUserId = request.getTargetUserId();
    var userResourceIds =
        userResourceDao.getResourceIdsByUserIdAndResourceType(
            targetUserId, request.getResourceType());
    userResourceIds.addAll(request.getResourceIds());
    var userResource =
        UserResourceEntityBuilder.newBuilder()
            .setSourceUserId(sourceUserId)
            .setUserId(targetUserId)
            .setResourceType(request.getResourceType())
            .setResourceIds(userResourceIds)
            .build();
    userResourceDao.insert(userResource);
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
        user.toBuilder()
            .setEmail(request.getEmail())
            .setLocale(request.getLocale())
            .setName(request.getName())
            .build();
    userDao.update(updatedUser);
    var loginUserResource = roleResourceDao.getResourceNamesByRoleName(user.getLoginRole());
    var tokens =
        tokenDao.listTokenByUserId(user.getId()).stream().map(TokenEntity::getTokenValue).toList();
    return UserConverter.INSTANCE
        .toUser(userDao.findById(request.getId()).orElseThrow())
        .loginRoleResources(loginUserResource)
        .tokens(tokens);
  }

  public I18nText verifyEmail(VerifyEmailRequest verifyEmailRequest) {
    // todo(yichi): send email according to locale information.
    try {
      emailClient.sendMail(
          verifyEmailRequest.getEmail(),
          "This is the verification email, please click the link below to verify this email.",
          "Email address verification");
    } catch (Exception e) {
      return UserFacingConstants.getConstant(EMAIL_SENDING_FAILED).getDisplayName();
    }
    return UserFacingConstants.getConstant(EMAIL_SENT).getDisplayName();
  }

  public VerifyByEmailCodeResponse verifyByCode(VerifyByEmailCodeRequest request) {
    var emailVerificationEntity=
        emailVerificationDao.getByEmailAndUserId(request.getEmail(), request.getUserId()).orElseThrow();
    boolean verifyResult = emailVerificationEntity.getEmailVerificationCode().equals(request.getVerificationCode()) &&
        !emailVerificationEntity.getIsUsed() && emailVerificationEntity.getExpirationTimestamp()>clock.millis();
    return new VerifyByEmailCodeResponse().verifyResult(verifyResult);
  }
}
