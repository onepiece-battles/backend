package com.yichi.onepiece.Iam;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IamService {

  public boolean checkPermissionOfCurrentAccount(
      String resourceType, List<PermissionDefinition.PermissionEnum> permissionEnums) {
    return true;
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
