package com.yichi.onepiece.Account.Service;

import com.yichi.onepiece.Account.Model.AccountCondition;
import com.yichi.onepiece.Account.Model.AccountDao;
import com.yichi.onepiece.Account.Model.UserEntity;
import com.yichi.onepiece.Account.Model.UserEntityBuilder;
import com.yichi.onepiece.Exception.ExceptionUtils;
import com.yichi.onepiece.Utils.EncryptHelper;
import com.yichi.onepiece.model.LoginResponse;
import com.yichi.onepiece.model.LogoutResponse;
import com.yichi.onepiece.model.UserInfo;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.apache.xmlbeans.impl.xb.xsdschema.PatternDocument;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
  private static final String VALID_EMAIL_PATTERN="(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
  // register info
  private static final String DUPLICATE_USER_NAME =
      "user with name %s has already been created, please consider anther name";
  private static final String PASSWORD_DIFFERENT = "please confirm your password again";
  // login info
  private static final String LOGIN_FAIL = "please register first before login";
  private static final String EMAIL_HAS_BEEN_REGISTERED = "This email has already been registered";

  private static final String INVALID_EMAIL_ADDRESS = "This email address is invalid, please check it again";
  private static final String WRONG_PASSWORD = "wrong password, please try again";
  // logout info
  public static final String NO_USER_FOUND = "sign fail, no user found";
  // pure success for register, login and logout.
  public static final String SUCCESS = "SUCCESS";
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
   * @return register verification: DUPLICATE_USER_NAME/PASSWORD_DIFFERENT/REGISTER_SUCCESS
   */
  public String register(
      String userName, String password, String email, String confirmedPassword, String organization) {
    if (!accountDao.listByName(userName).isEmpty()) {
      return String.format(DUPLICATE_USER_NAME, userName);
    }

    if (!accountDao.listByEmailAddress(email).isEmpty()) {
      return EMAIL_HAS_BEEN_REGISTERED;
    }

    if (!email.matches(VALID_EMAIL_PATTERN)) {
      return INVALID_EMAIL_ADDRESS;
    }
    if (!password.equals(confirmedPassword)) {
      return PASSWORD_DIFFERENT;
    }

    var encryptedPassword = EncryptHelper.getMd5String(password);
    UserEntity user =
        UserEntityBuilder.newBuilder()
            .setUserName(userName)
            .setAccountCondition(AccountCondition.Registered)
            .setEncryptedPassword(encryptedPassword)
            .setOrganization(organization)
            .build();
    accountDao.insert(user);
    return SUCCESS;
  }
  public LoginResponse login(String userName, String password, String email, String org) {
    var users = accountDao.listByName(userName);
    var userByEmail = accountDao.listByEmailAddress(email);
    users.addAll(userByEmail);
    if (users.isEmpty()) {
      return new LoginResponse().verification(LOGIN_FAIL);
    }

    var userEntity = users.get(0);
    if (!EncryptHelper.getMd5String(password).equals(userEntity.getEncryptedPassword())) {
      return new LoginResponse().verification(WRONG_PASSWORD);
    }
    var userLogin =
        userEntity.toBuilder()
            .setOrganization(org)
            .setAccountCondition(AccountCondition.SignIn)
            .build();
    accountDao.update(userLogin);
    return new LoginResponse()
        .verification(SUCCESS)
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
