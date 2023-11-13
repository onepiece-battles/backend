package com.yichi.onepiece.user;

import com.yichi.onepiece.model.UserRole;
import com.yichi.onepiece.user.model.UserDetailImpl;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserHelper {
  public static UUID getCurrentUserId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    UserDetailImpl userDetail = (UserDetailImpl) authentication.getPrincipal();
    return userDetail.getId();
  }

  public static UserDetailImpl getCurrentUserDetail() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return (UserDetailImpl) authentication.getPrincipal();
  }

  public static boolean hasNikaPermission() {
    return getCurrentUserDetail().getLoginRole().toString().equals(UserRole.NIKA.toString());
  }

  public static boolean hasAdminPermission() {
    return List.of(UserRole.NIKA.toString(), UserRole.ADMIN.toString())
        .contains(getCurrentUserDetail().getLoginRole().toString());
  }
}
