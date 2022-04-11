package com.yichi.onepiece.Account.Service;

import com.yichi.onepiece.Account.Model.AccountCondition;
import com.yichi.onepiece.Account.Model.AccountDao;
import com.yichi.onepiece.Account.Model.UserEntity;
import com.yichi.onepiece.Account.Model.UserEntityBuilder;
import com.yichi.onepiece.Utils.EncryptHelper;
import com.yichi.onepiece.model.LoginResponse;
import com.yichi.onepiece.model.LogoutResponse;
import com.yichi.onepiece.model.UserInfo;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
  // register info
  private static final String DUPLICATE_USER_NAME =
      "user with name %s has already been created, please consider anther name";
  private static final String PASSWORD_DIFFERENT = "please confirm your password again";
  // login info
  private static final String LOGIN_FAIL = "please register first before login";
  private static final String WRONG_PASSWORD = "wrong password, please try again";
  // logout info
  private static final String NO_USER_FOUND = "sign fail, no user found";
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
   * @param confirmedPassword confirmed password
   * @param organization organization user belongs to
   * @return register verification: DUPLICATE_USER_NAME/PASSWORD_DIFFERENT/REGISTER_SUCCESS
   */
  public String register(
      String userName, String password, String confirmedPassword, String organization) {
    if (!accountDao.listByName(userName).isEmpty()) {
      return String.format(DUPLICATE_USER_NAME, userName);
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

  public LoginResponse login(String userName, String password, String org) {
    var users = accountDao.listByName(userName);
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
}
