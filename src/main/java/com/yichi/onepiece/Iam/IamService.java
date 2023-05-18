package com.yichi.onepiece.Iam;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class IamService {
  private static final ThreadLocal<IamCache> cache = new ThreadLocal<>();

  public UUID getCurrentUserId() {
    IamCache cache = getCache();
    if (cache.currentUserID != null) {
      return cache.currentUserID;
    }
    if (SecurityContextHolder.getContext() != null) {
      var authentication = SecurityContextHolder.getContext();
      if (authentication instanceof UsernamePasswordAuthenticationToken) {
        cache.currentUserID =
            UUID.fromString(
                ((UsernamePasswordAuthenticationToken) authentication).getPrincipal().toString());
        return cache.currentUserID;
      }
    }
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  }

  IamCache getCache() {
    IamCache iamCache = cache.get();
    if (iamCache == null) {
      iamCache = new IamCache();
      cache.set(iamCache);
    }
    return iamCache;
  }

  void invalidateCache() {
    cache.remove();
  }

  static class IamCache {
    UUID currentUserID;
    UUID isNika;
  }
}
