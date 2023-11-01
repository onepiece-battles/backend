package com.yichi.onepiece.iam.service;

import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.iam.model.*;
import com.yichi.onepiece.model.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RoleService {
  private final RoleDao roleDao;
  private final RoleResourceDao roleResourceDao;
  private final ResourceDao resourceDao;

  public RoleService(RoleDao roleDao, RoleResourceDao roleResourceDao, ResourceDao resourceDao) {
    this.roleDao = roleDao;
    this.roleResourceDao = roleResourceDao;
    this.resourceDao = resourceDao;
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

  public AssignResourceToRoleResponse assignResourceToRole(AssignResourceToRoleRequest request) {
    var roleId = request.getRoleId();
    var roleEntity =
        roleDao
            .findById(roleId)
            .orElseThrow(ExceptionUtils.notFound("Role with id %s not found", roleId));
    var resourceIds =
        request.getResourcePermissionEntryList().stream()
            .map(ResourcePermissionsEntry::getResourceId)
            .toList();
    var resourceList = resourceDao.batchFindById(resourceIds);
    var sourceResourcePermissionsEntryList = request.getResourcePermissionEntryList();
    HashMap<UUID, Set<String>> validationMap = new HashMap<>();
    for (ResourceEntity resourceEntity : resourceList) {
      validationMap.put(resourceEntity.getId(), resourceEntity.getPermissions());
    }
    validateResourceAndPermissions(validationMap, sourceResourcePermissionsEntryList);
    List<RoleResourceEntity> assignedEntityList = new ArrayList<>();
    for (ResourcePermissionsEntry resourcePermissionsEntry : sourceResourcePermissionsEntryList) {
      var roleResourceEntity =
          RoleResourceEntityBuilder.newBuilder()
              .setResourceId(resourcePermissionsEntry.getResourceId())
              .setRoleId(request.getRoleId())
              .setPermissions(resourcePermissionsEntry.getPermissions())
              .build();
      assignedEntityList.add(roleResourceEntity);
    }
    roleResourceDao.batchInsert(assignedEntityList);
    var roleResourceEntities =
        roleResourceDao.listRoleResourceByRoleIds(List.of(request.getRoleId()));
    var resourcePermissionsEntryList =
        roleResourceEntities.stream()
            .map(
                roleResourceEntity -> {
                  ResourcePermissionsEntry resourcePermissionsEntry =
                      new ResourcePermissionsEntry();
                  resourcePermissionsEntry.setResourceId(roleResourceEntity.getResourceId());
                  resourcePermissionsEntry.setPermissions(roleResourceEntity.getPermissions());
                  return resourcePermissionsEntry;
                })
            .toList();
    return new AssignResourceToRoleResponse()
        .roleId(roleId)
        .roleName(roleEntity.getName())
        .resourcePermissionEntryList(resourcePermissionsEntryList);
  }

  private void validateResourceAndPermissions(
      HashMap<UUID, Set<String>> validationMap,
      List<ResourcePermissionsEntry> resourcePermissionsEntryList) {
    for (ResourcePermissionsEntry resourcePermissionsEntry : resourcePermissionsEntryList) {
      if (!validationMap
          .get(resourcePermissionsEntry.getResourceId())
          .containsAll(resourcePermissionsEntry.getPermissions())) {
        ExceptionUtils.notFound(
                "Invalid permission exists when validating resource, id: %s",
                resourcePermissionsEntry.getResourceId())
            .get();
      }
    }
  }
}
