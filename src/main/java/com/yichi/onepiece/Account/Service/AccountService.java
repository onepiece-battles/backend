package com.yichi.onepiece.Account.Service;

import com.yichi.onepiece.Account.Model.AccountCondition;
import com.yichi.onepiece.Account.Model.AccountDao;
import com.yichi.onepiece.Account.Model.UserEntity;
import com.yichi.onepiece.Account.Model.UserEntityBuilder;
import com.yichi.onepiece.Exception.ExceptionUtils;
import com.yichi.onepiece.UFC.UserFacingConstants;
import com.yichi.onepiece.UFC.UserFacingConstantsKey;
import com.yichi.onepiece.Utils.EncryptHelper;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.LoginResponse;
import com.yichi.onepiece.model.LogoutResponse;
import com.yichi.onepiece.model.UserInfo;

import java.util.UUID;

import org.springframework.stereotype.Service;

import static com.yichi.onepiece.Constants.ConstantsContainer.SUCCESS;
import static com.yichi.onepiece.Constants.ConstantsContainer.VALID_EMAIL_PATTERN;
import static com.yichi.onepiece.UFC.UserFacingConstantsKey.*;

@Service
public class AccountService {
  private final AccountDao accountDao;

  public AccountService(AccountDao accountDao) {
    this.accountDao = accountDao;
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
      String userName, String password, String email, String confirmedPassword, String organization) {
    if (!accountDao.listByName(userName).isEmpty()) {
      return UserFacingConstantsKey.DUPLICATE_USER_NAME;
    }
    if (!accountDao.listByEmailAddress(email).isEmpty()) {
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
            .setUserName(userName)
            .setAccountCondition(AccountCondition.Registered)
            .setEncryptedPassword(encryptedPassword)
                .setEmail(email)
            .setOrganization(organization)
            .build();
    accountDao.insert(user);
    return SUCCESS;
  }
  public LoginResponse login(String userName, String password, String email, String org) {
    var users = accountDao.listByName(userName);
    var userByEmail = accountDao.listByEmailAddress(email);
    users.addAll(userByEmail);
    I18nText verification;
    if (users.isEmpty()) {
      verification = UserFacingConstants.getConstant(REGISTER_FIRST_BEFORE_LOGIN).getDisplayName();
      return new LoginResponse().verification(verification);
    }

    var userEntity = users.get(0);
    if (!EncryptHelper.getMd5String(password).equals(userEntity.getEncryptedPassword())) {
      verification = UserFacingConstants.getConstant(WRONG_PASSWORD).getDisplayName();
      return new LoginResponse().verification(verification);
    }
    var userLogin =
        userEntity.toBuilder()
            .setOrganization(org)
            .setAccountCondition(AccountCondition.SignIn)
            .build();
    accountDao.update(userLogin);
    return new LoginResponse()
        .verification(null)
        .userInfo(
            new UserInfo()
                .userId(userLogin.getId())
                .userName(userLogin.getUserName())
                .organization(userLogin.getOrganization()));
  }

  public LogoutResponse logout(UUID userId) {
    var userOptional = accountDao.findById(userId);
    if (userOptional.isEmpty()) {
      return new LogoutResponse().logoutResult(NO_USER_FOUND);
    }

    var user = userOptional.get();
    var signedOutUser = user.toBuilder().setAccountCondition(AccountCondition.SignOut).build();
    accountDao.update(signedOutUser);
    return new LogoutResponse().logoutResult(SUCCESS).userName(user.getUserName());
  }

  public void justifyUser(UUID userId) {
    var userEntity =
        accountDao
            .findById(userId)
            .orElseThrow(ExceptionUtils.notFound("User with id: %s not found in database", userId));
    if (!AccountCondition.SignIn.equals(userEntity.getAccountCondition())) {
      throw ExceptionUtils.notFound(
              "Account condition of user with id: %s invalid, account condition: %s",
              userId, userEntity.getAccountCondition())
          .get();
    }
  }
}
