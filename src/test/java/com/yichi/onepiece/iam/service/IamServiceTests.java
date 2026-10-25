package com.yichi.onepiece.iam.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.iam.model.ResourcePermissionDao;
import com.yichi.onepiece.iam.model.RoleDao;
import com.yichi.onepiece.iam.model.RoleToResourcePermissionDao;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.UserRole;
import com.yichi.onepiece.user.model.UserDetailImpl;
import java.util.List;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testng.annotations.Test;

public class IamServiceTests {
  @Test
  public void rejectsLoginRoleMissingFromAssignedAuthorities() {
    var roleDao = mock(RoleDao.class);
    var permissionDao = mock(ResourcePermissionDao.class);
    var rolePermissionDao = mock(RoleToResourcePermissionDao.class);
    var service = new IamService(roleDao, permissionDao, rolePermissionDao);
    var user = new UserDetailImpl();
    user.setLoginRole(UserRole.ADMIN);
    user.setUserRoles(List.of(UserRole.NORMAL.name()));
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    SecurityContextHolder.setContext(context);
    try {
      assertThatThrownBy(() -> service.checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of()))
          .isInstanceOf(PermissionDeniedException.class);
      verifyNoInteractions(roleDao, permissionDao, rolePermissionDao);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
