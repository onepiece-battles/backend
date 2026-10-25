package com.yichi.onepiece.iam.service;

import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.iam.model.*;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.UserRole;
import com.yichi.onepiece.user.UserHelper;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class IamService {
  Logger logger = LoggerFactory.getLogger(IamService.class);
  private final RoleDao roleDao;
  private final ResourcePermissionDao resourcePermissionDao;
  private final RoleToResourcePermissionDao roleToResourcePermissionDao;

  public IamService(RoleDao roleDao, ResourcePermissionDao resourcePermissionDao,
      RoleToResourcePermissionDao roleToResourcePermissionDao) {
    this.roleDao = roleDao;
    this.resourcePermissionDao = resourcePermissionDao;
    this.roleToResourcePermissionDao = roleToResourcePermissionDao;
  }

  public void checkPermissionOfCurrentAccount(
      ResourceType resourceType, Set<Permission> permissions) {
    var authorities =
        Arrays.stream(
                SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getAuthorities()
                    .toArray(new GrantedAuthority[0]))
            .toList();
    if (authorities.isEmpty()) {
      throw ExceptionUtils.emptyAuthority(
          String.format("no authority found when checking the resource type %s", resourceType));
    }
    if (!(authorities.get(0) instanceof SimpleGrantedAuthority)) {
      throw ExceptionUtils.unexpectedAuthorityType(
          String.format("unexpected authority type %s", authorities.get(0).getClass()));
    }
    var roleNames = authorities.stream().map(GrantedAuthority::getAuthority).toList();
    var loginRole = UserHelper.getCurrentUserDetail().getLoginRole();
    if (!roleNames.contains(loginRole.name())) {
      throw new PermissionDeniedException("Access Denied");
    }
    for(var permission: permissions) {
      checkPermission(resourceType, permission, loginRole);
    }
  }

  private void checkPermission(
      ResourceType resourceType, Permission permission, UserRole userRole) {
    var role = roleDao.findByRoleName(userRole).orElseThrow();
    var resourcePermission = resourcePermissionDao.findByResourceAndPermission(resourceType, permission).orElseThrow();
    var result = roleToResourcePermissionDao.checkPermission(role.getId(), resourcePermission.getId());
    if(result.isEmpty()) {
      throw new PermissionDeniedException("Access Denied");
    }
  }
}
