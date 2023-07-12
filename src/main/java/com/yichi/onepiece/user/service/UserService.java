package com.yichi.onepiece.user.service;

import static com.yichi.onepiece.constants.ConstantsContainer.SUCCESS;
import static com.yichi.onepiece.constants.ConstantsContainer.VALID_EMAIL_PATTERN;
import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.*;

import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.iam.model.*;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.LoginResponse;
import com.yichi.onepiece.model.LogoutResponse;
import com.yichi.onepiece.model.User;
import com.yichi.onepiece.user.model.*;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;
import com.yichi.onepiece.utils.JwtHelper;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {
  private final UserDao userDao;
  private final TokenDao tokenDao;

  private final RoleDao roleDao;

  public UserService(UserDao userDao, TokenDao tokenDao, RoleDao roleDao) {
    this.userDao = userDao;
    this.tokenDao = tokenDao;
    this.roleDao = roleDao;
  }

  /**
   * Request to register into system
   *
   * @param userName username
   * @param password password
   * @param email email address
   * @param confirmedPassword confirmed password
   * @param organization organization user belongs to
   * @return register verification: key of DUPLICATE_USER_NAME/PASSWORD_DIFFERENT/REGISTER_SUCCESS
   */
  public String register(
      String userName,
      String password,
      String email,
      String confirmedPassword,
      String organization) {
    if (!userDao.listByName(userName).isEmpty()) {
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

    var encryptedPassword = EncryptHelper.getMd5String(password);
    UserEntity user =
        UserEntityBuilder.newBuilder()
            .setName(userName)
            // newly registered users are default to have role: "USER"
            .setUserRoles(Set.of(UserRole.USER.toString()))
            .setAccountCondition(UserAccountCondition.Registered)
            .setEncryptedPassword(encryptedPassword)
            .setEmail(email)
            .setOrganization(organization)
            .build();
    userDao.insert(user);
    return SUCCESS;
  }

  public LoginResponse login(String userName) {
    var users = userDao.listByName(userName);
    I18nText verification;
    if (users.isEmpty()) {
      verification = UserFacingConstants.getConstant(REGISTER_FIRST_BEFORE_LOGIN).getDisplayName();
      return new LoginResponse().verification(verification);
    }
    var user = (UserDetailImpl) users.get(0);

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

    var userLogin = user.toBuilder().setAccountCondition(UserAccountCondition.SignIn).build();
    userDao.update(userLogin);
    return new LoginResponse()
        .verification(null)
        .accessToken(jwtToken)
        .refreshToken(jwtRefreshToken)
        .userInfo(
            new User()
                .id(userLogin.getId())
                .name(userLogin.getName())
                .organization(userLogin.getOrganization()));
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

  public byte[] updateProfile(UUID userId, MultipartFile profile) {
    Byte[] profileByte = new Byte[0];
    try {
      profileByte = ArrayUtils.toObject(profile.getInputStream().readAllBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
    var user =
        userDao
            .findById(userId)
            .orElseThrow(ExceptionUtils.notFound("user with id %s not found", userId));
    var updatedUser = user.toBuilder().setProfileImage(profileByte).build();
    userDao.update(updatedUser);
    return ArrayUtils.toPrimitive(updatedUser.getProfileImage());
  }

  public byte[] getProfile(UUID userId) {
    var user =
        userDao
            .findById(userId)
            .orElseThrow(ExceptionUtils.notFound("user with id %s not found", userId));
    return ArrayUtils.toPrimitive(user.getProfileImage());
  }

  public void assignRoleToUser(UUID userId, Set<String> roles) {
    var user =
        userDao
            .findById(userId)
            .orElseThrow(ExceptionUtils.notFound("user with id %s not found", userId));
    var roleNameFullSet =
        roleDao.listAllRoles().stream()
            .map(RoleEntity::getName)
            .collect(Collectors.toCollection(HashSet::new));
    if (!roleNameFullSet.containsAll(roles)) {
      roles.addAll(user.getUserRoles());
      userDao.update(user.toBuilder().setUserRoles(roles).build());
    } else {
      throw ExceptionUtils.notFound(
              "Role name not found in db when assigning role to user %s", userId)
          .get();
    }
  }
}
