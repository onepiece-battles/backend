package com.yichi.onepiece.iam.service;

import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.iam.PermissionDefinition;
import com.yichi.onepiece.iam.PermissionDefinition.PermissionEnum;
import com.yichi.onepiece.iam.model.*;
import com.yichi.onepiece.model.UserRole;
import com.yichi.onepiece.user.UserHelper;
import java.util.*;
import java.util.stream.Collectors;
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
  private final RoleResourceDao roleResourceDao;
  private final ResourceDao resourceDao;

  public IamService(RoleDao roleDao, RoleResourceDao roleResourceDao, ResourceDao resourceDao) {
    this.roleDao = roleDao;
    this.roleResourceDao = roleResourceDao;
    this.resourceDao = resourceDao;
  }

  public void checkPermissionOfCurrentAccount(
      String resourceType, Set<PermissionEnum> permissionEnums) {
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
    if (!roleNames.contains(loginRole)) {
      ExceptionUtils.notFound("role name %s not found", loginRole).get();
    }
    var permissionList =
        permissionEnums.stream()
            .map(PermissionDefinition.PermissionEnum::toString)
            .collect(Collectors.toSet());
    checkPermission(resourceType, permissionList, List.of(loginRole));
  }

  private void checkPermission(
      String resourceType, Set<String> permissionEnums, List<UserRole> roleNames) {
    var roleEntities = roleDao.listRoleByRoleNames(roleNames);
    var roleResourceEntities =
        roleResourceDao.listRoleResourceByRoleIds(
            roleEntities.stream().map(RoleEntity::getId).toList());
    var resourceIds = roleResourceEntities.stream().map(RoleResourceEntity::getResourceId).toList();
    var resource =
        resourceDao.batchFindById(resourceIds).stream()
            .filter(resourceEntity -> resourceType.equals(resourceEntity.getName()))
            .findFirst();
    if (resource.isEmpty()) {
      throw new PermissionDeniedException(
          String.format("Resource with type %s not found", resourceType));
    } else {
      var resourceEntity = resource.get();
      var userPermissions =
          new HashSet<>(
              roleResourceEntities.stream()
                  .filter(
                      roleResourceEntity -> resourceEntity.getId().equals(resourceEntity.getId()))
                  .flatMap(roleResourceEntity -> roleResourceEntity.getPermissions().stream())
                  .toList());
      var allPermissionsValidated = userPermissions.containsAll(permissionEnums);
      if (!allPermissionsValidated) {
        throw new PermissionDeniedException("Not all permissions are allowed");
      }
    }
  }
}
