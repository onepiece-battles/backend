package com.yichi.onepiece.user;

import com.yichi.onepiece.user.model.UserDetailImpl;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserHelper {
  public static UUID getCurrentUserId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    UserDetailImpl userDetail = (UserDetailImpl) authentication.getPrincipal();
    return userDetail.getId();
  }

  public static UserDetailImpl getCurrentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return (UserDetailImpl) authentication.getPrincipal();
  }
}
