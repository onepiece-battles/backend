package com.yichi.onepiece.user.service;

import static com.yichi.onepiece.constants.OnepieceConstants.*;
import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.*;

import com.yichi.onepiece.email.EmailService;
import com.yichi.onepiece.quartz.jobs.EmailJob;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.iam.model.*;
import com.yichi.onepiece.iam.service.RoleService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.model.UserRole;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.*;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final Logger logger = LoggerFactory.getLogger(UserService.class);
  private final UserDao userDao;
  private final TokenDao tokenDao;
  private final RoleDao roleDao;
  private final UserRoleDao userRoleDao;
  private final PasswordEncoder passwordEncoder;
  private final UserResourceDao userResourceDao;
  private final ActionRecordService actionRecordService;
  private final RoleService roleService;

  private final EmailVerificationDao emailVerificationDao;
  private final Clock clock;

  public UserService(
          UserDao userDao,
          TokenDao tokenDao,
          RoleDao roleDao,
          UserRoleDao userRoleDao,
          PasswordEncoder passwordEncoder,
          UserResourceDao userResourceDao,
          ActionRecordService actionRecordService,
          EmailService emailService,
          RoleService roleService,
          EmailVerificationDao emailVerificationDao,
          Clock clock) {
    this.userDao = userDao;
    this.tokenDao = tokenDao;
    this.roleDao = roleDao;
    this.userRoleDao = userRoleDao;
    this.passwordEncoder = passwordEncoder;
    this.userResourceDao = userResourceDao;
    this.actionRecordService = actionRecordService;
    this.roleService = roleService;
    this.emailVerificationDao = emailVerificationDao;
    this.clock = clock;
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

  public void updateProfile(UUID userId, String uri) {
    var modificationRecord = actionRecordService.create();
    var user =
            userDao
                    .findById(userId)
                    .orElseThrow(ExceptionUtils.notFound("user with id %s not found", userId));
    var updatedUser = user.toBuilder().setImageUri(uri).setModificationRecordId(modificationRecord.getId()).build();
    userDao.update(updatedUser);
  }

  public ListUsersResponse list(ListUsersRequest request) {
    var totalSize = userDao.searchCount(request.getUserFilter());
    // pageIndex is 1- based. offset is 0- based.
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return new ListUsersResponse().users(List.of()).totalSize(totalSize);
    }
    var userTemp = userDao
            .listUsers(
                    request.getUserFilter(), request.getSortKeys(), offset, request.getPageSize());
    logger.info(userTemp.toString());
    var users =
        userDao
            .listUsers(
                request.getUserFilter(), request.getSortKeys(), offset, request.getPageSize())
            .stream()
            .map(UserConverter.INSTANCE::toUser)
            .toList();
    return new ListUsersResponse().users(users).totalSize(totalSize);
  }


  public List<UserEntity> listNoUriUsers() {
    return userDao.listEmptyUriUsers();
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
            .resourcePermissionMap(roleService.getPermissionMapByRoleName(role));
  }

  public User getUserById(UUID userId) {
    var user = userDao.getById(userId).orElseThrow();
    return UserConverter.INSTANCE.toUser(user)
            .loginRoleResourcesPermissionMap(roleService.getPermissionMapByRoleName(user.getLoginRole()));
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
    var loginUserResource = roleService.getPermissionMapByRoleName(user.getLoginRole());
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
