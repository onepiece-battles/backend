package com.yichi.onepiece.user.service;

import static com.yichi.onepiece.constants.OnepieceConstants.*;
import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.*;

import com.yichi.onepiece.quartz.jobs.EmailJob;
import com.yichi.onepiece.constants.OnepieceConstants;
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

import java.io.IOException;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.ArrayUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final Logger logger = LoggerFactory.getLogger(UserService.class);
  private final UserDao userDao;
  private final AuthenticationManager authenticationManager;
  private final TokenDao tokenDao;
  private final RoleDao roleDao;
  private final UserRoleDao userRoleDao;
  private final PasswordEncoder passwordEncoder;
  private final UserResourceDao userResourceDao;
  private final ActionRecordService actionRecordService;
  private final SchedulerFactoryBean schedulerFactoryBean;
  private final RoleService roleService;

  private final EmailVerificationDao emailVerificationDao;
  private final Clock clock;

  public UserService(
          UserDao userDao,
          AuthenticationManager authenticationManager,
          TokenDao tokenDao,
          RoleDao roleDao,
          UserRoleDao userRoleDao,
          PasswordEncoder passwordEncoder,
          UserResourceDao userResourceDao,
          ActionRecordService actionRecordService,
          SchedulerFactoryBean schedulerFactoryBean,
          RoleService roleService,
          EmailVerificationDao emailVerificationDao,
          Clock clock) {
    this.userDao = userDao;
    this.authenticationManager = authenticationManager;
    this.tokenDao = tokenDao;
    this.roleDao = roleDao;
    this.userRoleDao = userRoleDao;
    this.passwordEncoder = passwordEncoder;
    this.userResourceDao = userResourceDao;
    this.actionRecordService = actionRecordService;
    this.schedulerFactoryBean = schedulerFactoryBean;
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
   * @param onepieceLocale the locale information (i18n information) based on FE language
   * @return register verification: key of DUPLICATE_USER_NAME/PASSWORD_DIFFERENT/REGISTER_SUCCESS
   */
  public String register(
      String nickName,
      String password,
      String username,
      String confirmedPassword,
      OnepieceLocale onepieceLocale,
      @Nullable UserRole userRole)  {
    if (!userDao.listByNickName(nickName).isEmpty()) {
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
    UserEntity user;
    try {
      var defaultImage =  new ClassPathResource("/images/user_default_profile.png").getContentAsByteArray();
      var encryptedPassword = passwordEncoder.encode(password);
      user = UserEntityBuilder.newBuilder()
                      .setNickname(nickName)
                      .setAccountStatus(AccountStatus.LOGGED_OUT)
                      .setEncryptedPassword(encryptedPassword)
                      .setUsername(username)
                      .setEmailVerified(false)
                      .setLocale(onepieceLocale)
                      .setLoginRole(null)
                      .setLoginRoleId(null)
                      .setProfileImage(ArrayUtils.toObject(defaultImage))
                      .build();
    } catch (IOException e) {
      throw new RuntimeException();
    }
    userDao.insert(user);
    // new user are default to be created by NIKA
    var actionRecord = actionRecordService.createWithUserAndRole(user, UserRole.NIKA);
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
    logger.info("start login");
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

    // update user login role id
    var roles = roleDao.listByUserId(user.getId());
    var roleEntity = getHighestPermissionRole(roles);
    user.setAccountStatus(AccountStatus.LOGGED_IN);
    user.setLoginRole(roleEntity.getName());
    user.setLoginRoleId(roleEntity.getId());
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

  private RoleEntity getHighestPermissionRole(List<RoleEntity> roles) {
    if (roles.isEmpty()) {
      throw ExceptionUtils.notFound("no role assigned to user").get();
    }
    Map<UserRole, RoleEntity> roleNameToRole = roles.stream()
            .collect(Collectors.toMap(RoleEntity::getName, Function.identity()));
    var selectedRole = Stream.of(UserRole.NIKA, UserRole.ADMIN, UserRole.NORMAL, UserRole.VISITOR).filter(
            roleNameToRole::containsKey
    ).findFirst().orElseThrow();
    return roleNameToRole.get(selectedRole);
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

  public PasswordRetrievalResponse sendRetrievalEmail(PasswordRetrievalRequest request) {
    emailVerificationDao.markEmailAsUsedNullUserId(request.getEmail());
    long creationTimestamp = clock.millis();
    var verificationCode = RandomGenerator.generateVerificationCode();
    String subject = "";
    String body = "";
    if(OnepieceLocale.EN_US.equals(request.getLocale())) {
      subject = "[Onepiece Battles] Reset Password";
      body = String.format("This is the verification email, The verification code is: %s", verificationCode);
    } else {
      subject = "[Onepiece Battles] 找回密码";
      body = String.format("这是您的验证邮件，验证码为：%s", verificationCode);
    }
    var text = sendEmail(request.getEmail(), subject, body);
    // expires 5 minutes later
    long expirationTimestamp = creationTimestamp + 300000L;
    var emailVerificationEntity =
            EmailVerificationEntityBuilder.newBuilder()
                    .setEmail(request.getEmail())
                    .setUserId(null)
                    .setEmailVerificationCode(verificationCode)
                    .setCreationTimestamp(clock.millis())
                    .setExpirationTimestamp(expirationTimestamp)
                    .isUsed(false)
                    .build();
    emailVerificationDao.insert(emailVerificationEntity);
    boolean result = text.equals(UserFacingConstants.getConstant(EMAIL_SENT).getDisplayName());
    return new PasswordRetrievalResponse().result(result).notification(text);
  }

  public UpdatePasswordResponse updatePassword(UpdatePasswordRequest request) {
    var result = justifyVerificationCode(null, request.getUsername(), request.getVerificationCode());
    if (!VERIFICATION_SUCCESS.equals(result)) {
      return new UpdatePasswordResponse()
              .result(false).notification(UserFacingConstants.getConstant(
                      result).getDisplayName());
    }
    var userOptional = userDao.getByUsername(request.getUsername());
    if (userOptional.isPresent()) {
      var user = userOptional.get();
      user = user.toBuilder().setEncryptedPassword(passwordEncoder.encode(request.getPassword())).build();
      userDao.update(user);
      return new UpdatePasswordResponse().result(true).notification(
              UserFacingConstants.getConstant(
                      PASSWORD_UPDATED_SUCCESS).getDisplayName());
    } else {
      return new UpdatePasswordResponse().result(false).notification(
              UserFacingConstants.getConstant(
                      UserFacingConstantsKey.EMAIL_HAS_BEEN_REGISTERED).getDisplayName());
    }
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

  public UpdateLoginRoleResponse updateLoginRole(UserRole role) {
    var user = UserHelper.getCurrentUserDetail();
    if (!user.getUserRoles().contains(role.toString())) {
      ExceptionUtils.notFound("user role %s not found when shifting role.", role).get();
    }
    var updatedRole = roleDao.findByRoleName(role).orElseThrow();
    user.setLoginRole(role);
    user.setLoginRoleId(updatedRole.getId());
    userDao.update(user);
    return new UpdateLoginRoleResponse()
            .currentRole(role)
            .currentRoleId(updatedRole.getId())
            .resourcePermissionMap(getPermissionMapByRoleName(role));
  }

  public User getUserById(UUID userId) {
    var user = userDao.getById(userId).orElseThrow();
    return UserConverter.INSTANCE.toUser(user)
            .loginRoleResourcesPermissionMap(getPermissionMapByRoleName(user.getLoginRole()));
  }

  public UserEntity getUserEntityById(UUID userId) {
    return userDao.getById(userId).orElseThrow();
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

  public VerifyEmailAddressResponse verifyEmailAddress(VerifyEmailAddressRequest request) {
    // id from  request is user id
    emailVerificationDao.markEmailAsUsed(request.getId(), request.getEmail());
    long creationTimestamp = clock.millis();
    var verificationCode = RandomGenerator.generateVerificationCode();
    var locale = UserHelper.getCurrentUserDetail().getLocale();
    String subject = "";
    String body = "";
    if(OnepieceLocale.EN_US.equals(locale)) {
      subject = "[Onepiece Battles] Email address verification";
      body = String.format("This is the verification email, The verification code is: %s", verificationCode);
    } else {
      subject = "[Onepiece Battles] 电子邮箱验证";
      body = String.format("这是您的验证邮件，验证码为：%s", verificationCode);
    }
    var text = sendEmail(request.getEmail(), subject, body);

    // expires 5 minutes later
    long expirationTimestamp = creationTimestamp + 300000L;
    var emailVerificationEntity =
            EmailVerificationEntityBuilder.newBuilder()
                    .setEmail(request.getEmail())
                    .setUserId(request.getId())
                    .setEmailVerificationCode(verificationCode)
                    .setCreationTimestamp(clock.millis())
                    .setExpirationTimestamp(expirationTimestamp)
                    .isUsed(false)
                    .build();
    emailVerificationDao.insert(emailVerificationEntity);
    // TODO: 2024/2/29 consider the case when email sent failed. 
    //boolean result = text.equals(UserFacingConstants.getConstant(EMAIL_SENT).getDisplayName());
    return new VerifyEmailAddressResponse().emailNotification(text);
  }

  public I18nText sendEmail(
          String email,
          String subject,
          String body) {
    try {
      JobDataMap jobDataMap = new JobDataMap();
      jobDataMap.put(RECEIVER_EMAIL_ADDRESS, email);
      jobDataMap.put(SUBJECT, subject);
      jobDataMap.put(BODY, body);

      JobDetail emailSendingJob = JobBuilder.newJob(EmailJob.class)
              .setJobData(jobDataMap)
              .withIdentity(UUID.randomUUID().toString(), EMAIL_JOB_GROUP)
              .withDescription("send email job")
              .build();
      Scheduler scheduler = schedulerFactoryBean.getScheduler();
      Trigger emailJobTrigger = TriggerBuilder.newTrigger()
              .forJob(emailSendingJob)
              .startNow().build();
      scheduler.start();
      scheduler.scheduleJob(emailSendingJob, emailJobTrigger);
    } catch (Exception e) {
      logger.error(e.toString());
      return UserFacingConstants.getConstant(EMAIL_SENDING_FAILED).getDisplayName();
    }
    return UserFacingConstants.getConstant(EMAIL_SENT).getDisplayName();
  }

  public VerifyByEmailCodeResponse verifyUserEmail(VerifyByEmailCodeRequest request) {
    var result =  justifyVerificationCode(request.getUserId(), request.getEmail(),request.getVerificationCode());
    if (!VERIFICATION_SUCCESS.equals(result)) {
      return new VerifyByEmailCodeResponse()
              .verifyResult(false).verifyMessage(UserFacingConstants.getConstant(
                      result).getDisplayName());
    }
    // if verification code is correct, send mark user email as verified.
    var user = userDao.findById(request.getUserId()).orElseThrow();
    user = user.toBuilder().setEmailVerified(true).build();
    userDao.update(user);
    return new VerifyByEmailCodeResponse()
        .verifyResult(true).verifyMessage(UserFacingConstants.getConstant(
        VERIFICATION_SUCCESS).getDisplayName());
  }

  private String justifyVerificationCode(@Nullable UUID userId, String email, String verificationCode) {
    List<EmailVerificationEntity> emailVerificationEntity;
    if (userId==null) {
      emailVerificationEntity= emailVerificationDao.getByEmailAndNullUserId(email, verificationCode);
    } else {
      emailVerificationEntity =emailVerificationDao
              .getByEmailAndUserId(email, userId, verificationCode);
    }
    if (emailVerificationEntity.isEmpty()) {
      return VERIFICATION_CODE_NOT_MATCH;
    }
    if (emailVerificationEntity.get(0).getExpirationTimestamp() < clock.millis()) {
      return VERIFICATION_CODE_EXPIRED;
    }
    // mark email as used.
    if (userId==null) {
      emailVerificationDao.markEmailAsUsedNullUserId(email);
    } else {
      emailVerificationDao.markEmailAsUsed(userId, email);
    }
    return VERIFICATION_SUCCESS;
  }
}
