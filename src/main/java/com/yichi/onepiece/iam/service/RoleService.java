package com.yichi.onepiece.iam.service;

import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.iam.model.*;
import com.yichi.onepiece.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RoleService {
  private final RoleDao roleDao;
  private final ResourcePermissionDao resourcePermissionDao;
  private final RoleToResourcePermissionDao roleToResourcePermissionDao;

  public RoleService(RoleDao roleDao, ResourcePermissionDao resourcePermissionDao,
      RoleToResourcePermissionDao roleToResourcePermissionDao) {
    this.roleDao = roleDao;
    this.resourcePermissionDao = resourcePermissionDao;
    this.roleToResourcePermissionDao = roleToResourcePermissionDao;
  }

  public AddRoleResponse addRole(AddRoleRequest addRoleRequest) {
    var role = RoleEntityBuilder.newBuilder().setName(addRoleRequest.getRoleName()).build();
    roleDao.insert(role);
    var roleList =
        roleDao.listAllRoles().stream().map(RoleEntity::getName).collect(Collectors.toList());
    return new AddRoleResponse().userRoles(roleList);
  }

  public RoleEntity getRoleByName(UserRole roleName) {
    return roleDao.findByRoleName(roleName).orElseThrow();
  }

  public Map<ResourceType, Set<Permission>> getResourcePermissionMapByRoleId(UUID roleId) {
    var resourcePermissions = resourcePermissionDao.listResourcePermissionsByRoleId(roleId);
    return resourcePermissions.stream()
            .collect(Collectors.groupingBy(ResourcePermissionEntity::getResourceType, Collectors.mapping(ResourcePermissionEntity::getPermission, Collectors.toSet())));
  }

  public AssignResourcePermissionsToRoleResponse assignResourceToRole(AssignResourcePermissionsToRoleRequest request) {
    var roleId = request.getRoleId();
    // check role existence
        roleDao
            .findById(roleId)
            .orElseThrow(ExceptionUtils.notFound("Role with id %s not found", roleId));
    var resource = request.getResourceType();
    var permissions = request.getPermissions();
    var roleToResourcePermissionEntities = resourcePermissionDao.
        listAll().stream().filter(
        resourcePermissionEntity ->
            resourcePermissionEntity.getResourceType()==resource
                && permissions.contains(resourcePermissionEntity.getPermission())
    ).map(ResourcePermissionEntity::getId).map(id-> RoleToResourcePermissionEntityBuilder.newBuilder()
        .setRoleId(roleId)
        .setResourcePermissionId(id)
        .build())
        .toList();
    roleToResourcePermissionDao.batchInsert(roleToResourcePermissionEntities);
    return new AssignResourcePermissionsToRoleResponse().result(new I18nText().enUS("success").zhCN("成功"));
  }
}
