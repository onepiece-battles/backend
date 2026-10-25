package com.yichi.onepiece.user.service;

import static com.yichi.onepiece.constants.OnepieceConstants.*;
import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.*;

import com.aliyuncs.utils.Base64Helper;
import com.yichi.onepiece.email.EmailService;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.notification.service.NotificationService;
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

import com.yichi.onepiece.utils.EncryptHelper;
import com.yichi.onepiece.utils.I18nHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  private final UserDao userDao;
  private final FriendshipDao friendshipDao;
  private final TokenDao tokenDao;
  private final RoleDao roleDao;
  private final UserRoleDao userRoleDao;
  private final PasswordEncoder passwordEncoder;
  private final UserResourceDao userResourceDao;
  private final ActionRecordService actionRecordService;
  private final RoleService roleService;
  private final NotificationService notificationService;

  private final EmailVerificationDao emailVerificationDao;
  private final Clock clock;

  public UserService(
          UserDao userDao, FriendshipDao friendshipDao,
          TokenDao tokenDao,
          RoleDao roleDao,
          UserRoleDao userRoleDao,
          PasswordEncoder passwordEncoder,
          UserResourceDao userResourceDao,
          ActionRecordService actionRecordService,
          EmailService emailService,
          RoleService roleService, NotificationService notificationService,
          EmailVerificationDao emailVerificationDao,
          Clock clock) {
    this.userDao = userDao;
      this.friendshipDao = friendshipDao;
      this.tokenDao = tokenDao;
    this.roleDao = roleDao;
    this.userRoleDao = userRoleDao;
    this.passwordEncoder = passwordEncoder;
    this.userResourceDao = userResourceDao;
    this.actionRecordService = actionRecordService;
    this.roleService = roleService;
      this.notificationService = notificationService;
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

  public void updateProfile(UUID userId, String uri, String image) {
    var modificationRecord = actionRecordService.create();
    var user =
            userDao
                    .findById(userId)
                    .orElseThrow(ExceptionUtils.notFound("user with id %s not found", userId));
    var updatedUser = user.toBuilder()
            .setProfileImage(ArrayUtils.toObject(EncryptHelper.base64Decode(image)))
            .setImageUri(uri).setModificationRecordId(modificationRecord.getId()).build();
    userDao.update(updatedUser);
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

  public ListFriendResponse listFriend(ListFriendRequest request) {
    UUID currentUserId = UserHelper.getCurrentUserId();
    List<FriendshipEntity> friendships = friendshipDao.listFriendshipsByUserId(currentUserId, FriendshipStatus.ACCEPTED);
    List<UUID> friendIds = new ArrayList<>();
    for(var fs : friendships) {
      if(currentUserId.equals(fs.getFriendLeftId())) {
        friendIds.add(fs.getFriendRightId());
      } else {
        friendIds.add(fs.getFriendLeftId());
      }
    }
    List<UserEntity> userEntities;
    if (!friendIds.isEmpty()) {
      userEntities = userDao.listByUserIds(friendIds);
    } else {
      userEntities = new ArrayList<>();
    }
    return new ListFriendResponse().users(userEntities.stream().map(e->UserConverter.INSTANCE.toUser(e)).toList());
  }

  public List<User> listByUserIds(List<UUID> ids) {
      return userDao.listByUserIds(ids)
              .stream()
            .map(UserConverter.INSTANCE::toUser)
            .toList();
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
    if (roleNameFullSet.containsAll(roles)) {
      List<UserRoleEntity> userRoleEntities = new ArrayList<>();
      var requestedRoleIds =
          roleEntities.stream()
              .filter(roleEntity -> roles.contains(roleEntity.getName()))
              .map(RoleEntity::getId)
              .toList();
      for (var roleId : requestedRoleIds) {
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
    if (!UserHelper.hasAdminPermission() && !UserHelper.getCurrentUserId().equals(userId)) {
      throw new PermissionDeniedException("Access Denied");
    }
    var user = userDao.getById(userId).orElseThrow();
    return UserConverter.INSTANCE.toUser(user)
            .profileImage(EncryptHelper.base64Encode(ArrayUtils.toPrimitive(user.getProfileImage())))
            .loginRoleResourcesPermissionMap(roleService.getPermissionMapByRoleName(user.getLoginRole()));
  }

  public UserEntity getUserEntityById(UUID userId) {
    return userDao.getById(userId).orElseThrow();
  }

  public ShareResourceResponse shareResource(ShareResourceRequest request) {
    UUID sourceUserId = UserHelper.getCurrentUserId();
    UUID targetUserId = request.getTargetUserId();
    var existingResourceIds = new HashSet<>(
        userResourceDao.getResourceIdsByUserIdAndResourceType(
            targetUserId, request.getResourceType()));
    var userResourceEntities =
        request.getResourceIds().stream()
            .distinct()
            .filter(id -> !existingResourceIds.contains(id))
            .map(
                id ->
                    UserResourceEntityBuilder.newBuilder()
                        .setUserId(targetUserId)
                        .setSourceUserId(sourceUserId)
                        .setResourceType(request.getResourceType())
                        .setResourceId(id)
                        .build())
            .toList();
    if (!userResourceEntities.isEmpty()) {
      userResourceDao.batchInsert(userResourceEntities);
    }
    ShareResourceResponse response = new ShareResourceResponse();
    response.setSharingInformation(
        UserFacingConstants.getConstant(RESOURCE_SHARING_SUCCESS).getDisplayName());
    return response;
  }

  public List<UUID> getResourceIdsByUserIdResourceType(UUID userId, ResourceType resourceType) {
    return userResourceDao.getResourceIdsByUserIdAndResourceType(userId, resourceType);
  }

  public User updateUser(UpdateUserRequest request) {
    if (!UserHelper.hasAdminPermission() && !UserHelper.getCurrentUserId().equals(request.getId())) {
      throw new PermissionDeniedException("Access Denied");
    }
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
    if (!UserHelper.getCurrentUserId().equals(request.getUserId())) {
      throw new PermissionDeniedException("Access Denied");
    }
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

  public AddFriendResponse addFriend(AddFriendRequest request) {
    var actionRecord = actionRecordService.create();
    FriendshipEntity friendshipEntity = FriendshipEntityBuilder.newBuilder()
            .setCreationRecordId(actionRecord.getId())
            .setModificationRecordId(actionRecord.getId())
            .friendLeftId(UserHelper.getCurrentUserId())
            .friendshipStatus(FriendshipStatus.APPLIED)
            .friendRightId(request.getFriendId())
            .build();
    friendshipDao.insert(friendshipEntity);
    // send target message
    UserEntity sender = userDao.findById(UserHelper.getCurrentUserId()).orElseThrow();
    CreateNotificationRequest notificationRequest = new CreateNotificationRequest()
            .notificationType(NotificationType.FRIEND_REQUEST)
                    .creatorId(UserHelper.getCurrentUserId())
                            .receiverId(request.getFriendId())
                                    .content(I18nHelper.getFriendRequestMessage(sender));
    notificationService.create(notificationRequest);
    return new AddFriendResponse().result(true).notification(I18nHelper.getSuccess());
  }




  public AcceptFriendResponse acceptFriend(AcceptFriendRequest request) {
    List<FriendshipEntity> friendships = friendshipDao.findFriendShipById(request.getFriendId(), UserHelper.getCurrentUserId());
    if (!friendships.isEmpty()) {
      friendships = friendships.stream().map(e-> e.toBuilder().setFriendshipStatus(FriendshipStatus.ACCEPTED).build()).toList();
    }
    for (var friend : friendships) {
      // 更新朋友信息
      friendshipDao.update(friend);
      UserEntity sender = userDao.findById(UserHelper.getCurrentUserId()).orElseThrow();
      CreateNotificationRequest notificationRequest = new CreateNotificationRequest()
              .notificationType(NotificationType.FRIEND_ACCEPTED)
              .creatorId(UserHelper.getCurrentUserId())
              .receiverId(request.getFriendId())
              .content(I18nHelper.getFriendAcceptMessage(sender));
      notificationService.create(notificationRequest);
    }
    return new AcceptFriendResponse()
            .result(true)
            .notification(I18nHelper.getSuccess());
  }

  public RejectFriendResponse rejectFriend(RejectFriendRequest request) {
    List<FriendshipEntity> friendships = friendshipDao.findFriendShipById(request.getFriendId(), UserHelper.getCurrentUserId());
    for (var friendship : friendships) {
      friendshipDao.update(friendship.toBuilder().setFriendshipStatus(FriendshipStatus.REJECTED).build());
    }
    UserEntity sender = userDao.findById(UserHelper.getCurrentUserId()).orElseThrow();
    CreateNotificationRequest notificationRequest = new CreateNotificationRequest()
            .notificationType(NotificationType.FRIEND_REQUEST)
            .creatorId(UserHelper.getCurrentUserId())
            .receiverId(request.getFriendId())
            .content(I18nHelper.getFriendRejectMessage(sender));
    notificationService.create(notificationRequest);
    return new RejectFriendResponse().notification(I18nHelper.getSuccess()).result(true);
  }
}
