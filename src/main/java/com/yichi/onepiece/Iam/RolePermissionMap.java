package com.yichi.onepiece.Iam;

import com.google.common.collect.ImmutableMap;
import com.yichi.onepiece.User.Model.UserRole;
import java.util.List;

public class RolePermissionMap {
  public static final ImmutableMap<UserRole, List<PermissionDefinition.PermissionEnum>>
      rolePermissionMap =
          ImmutableMap.of(
              UserRole.ADMIN, PermissionDefinition.getAllPermissionEnums(),
              UserRole.NIKA,
              PermissionDefinition.getAllPermissionEnums(),
              UserRole.USER,
              PermissionDefinition.getAllPermissionEnums());
}
