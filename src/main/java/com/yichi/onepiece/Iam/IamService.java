package com.yichi.onepiece.Iam;

import com.yichi.onepiece.Exception.ExceptionUtils;
import com.yichi.onepiece.Iam.model.*;
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

  public boolean checkPermissionOfCurrentAccount(
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
    if (authorities.get(0) instanceof SimpleGrantedAuthority) {
      var roleNames = authorities.stream().map(GrantedAuthority::getAuthority).toList();
      var roleEntities = roleDao.listRoleByRoleNames(roleNames);
      var roleResourceEntities =
          roleResourceDao.listRoleResourceByRoleIds(
              roleEntities.stream().map(RoleEntity::getId).toList());
      var resourceIds =
          roleResourceEntities.stream().map(RoleResourceEntity::getResourceId).toList();
      var resource =
          resourceDao.batchFindById(resourceIds).stream()
              .filter(resourceEntity -> resourceType.equals(resourceEntity.getName()))
              .findFirst();
      if (resource.isEmpty()) {
        return false;
      } else {
        var resourceEntity = resource.get();
        return new HashSet<>(
                roleResourceEntities.stream()
                    .filter(
                        roleResourceEntity -> resourceEntity.getId().equals(resourceEntity.getId()))
                    .flatMap(roleResourceEntity -> roleResourceEntity.getPermissions().stream())
                    .toList())
            .containsAll(
                permissionEnums.stream()
                    .map(PermissionDefinition.PermissionEnum::toString)
                    .toList());
      }
    } else {
      throw ExceptionUtils.unexpectedAuthorityType(
          String.format("unexpected authority type %s", authorities.get(0).getClass()));
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
