package com.yichi.onepiece.iam;

import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.iam.model.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class IamService {
  private final RoleDao roleDao;
  private final RoleResourceDao roleResourceDao;
  private final ResourceDao resourceDao;

  public IamService(RoleDao roleDao, RoleResourceDao roleResourceDao, ResourceDao resourceDao) {
    this.roleDao = roleDao;
    this.roleResourceDao = roleResourceDao;
    this.resourceDao = resourceDao;
  }

  public void checkPermissionOfCurrentAccount(
      String resourceType, List<PermissionDefinition.PermissionEnum> permissionEnums) {
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
    var permissionList =
        permissionEnums.stream().map(PermissionDefinition.PermissionEnum::toString).toList();
    checkPermission(resourceType, permissionList, roleNames);
  }

  public void checkPermission(
      String resourceType, List<String> permissionEnums, List<String> roleNames) {
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
      var allPermissionsValidated =
          new HashSet<>(
                  roleResourceEntities.stream()
                      .filter(
                          roleResourceEntity ->
                              resourceEntity.getId().equals(resourceEntity.getId()))
                      .flatMap(roleResourceEntity -> roleResourceEntity.getPermissions().stream())
                      .toList())
              .containsAll(permissionEnums);
      if (!allPermissionsValidated) {
        throw new PermissionDeniedException("Not all permissions are allowed");
      }
    }
  }
  //  private static final ThreadLocal<IamCache> cache = new ThreadLocal<>();
  //
  //  public UUID getCurrentUserId() {
  //    IamCache cache = getCache();
  //    if (cache.currentUserID != null) {
  //      return cache.currentUserID;
  //    }
  //    if (SecurityContextHolder.getContext() != null) {
  //      var authentication = SecurityContextHolder.getContext();
  //      if (authentication instanceof UsernamePasswordAuthenticationToken) {
  //        cache.currentUserID =
  //            UUID.fromString(
  //                ((UsernamePasswordAuthenticationToken)
  // authentication).getPrincipal().toString());
  //        return cache.currentUserID;
  //      }
  //    }
  //    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  //  }
  //
  //  IamCache getCache() {
  //    IamCache iamCache = cache.get();
  //    if (iamCache == null) {
  //      iamCache = new IamCache();
  //      cache.set(iamCache);
  //    }
  //    return iamCache;
  //  }
  //
  //  void invalidateCache() {
  //    cache.remove();
  //  }
  //
  //  static class IamCache {
  //    UUID currentUserID;
  //    UUID isNika;
  //  }
}
