package com.yichi.onepiece.configuration;

import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.iam.PermissionDefinition;
import com.yichi.onepiece.iam.model.*;
import com.yichi.onepiece.user.model.UserRole;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PrepareResourceAndPermissionData implements CommandLineRunner {
  Logger logger = LoggerFactory.getLogger(PrepareResourceAndPermissionData.class);
  private final ResourceDao resourceDao;
  private final RoleDao roleDao;
  private final RoleResourceDao roleResourceDao;

  public PrepareResourceAndPermissionData(
      ResourceDao resourceDao, RoleDao roleDao, RoleResourceDao roleResourceDao) {
    this.resourceDao = resourceDao;
    this.roleDao = roleDao;
    this.roleResourceDao = roleResourceDao;
  }

  @Override
  public void run(String... args) {
    // initialize resource and permission data
    List<ResourceEntity> resourceNewEntities = new ArrayList<>();
    List<ResourceEntity> resourceUpdatedEntities = new ArrayList<>();
    var resourcePermissionsMap = PermissionDefinition.getResourcePermissionMap();
    var resourceNames = resourceDao.listAllResourceNames();
    for (Map.Entry<String, Set<String>> entry : resourcePermissionsMap.entrySet()) {
      var resource =
          ResourceEntityBuilder.newBuilder()
              .setName(entry.getKey())
              .setPermissions(entry.getValue())
              .build();
      if (resourceNames.contains(entry.getKey())) {
        resourceUpdatedEntities.add(resource);
      } else {
        resourceNewEntities.add(resource);
      }
    }
    if (!resourceNewEntities.isEmpty()) {
      resourceDao.batchInsert(resourceNewEntities);
    }
    for (var resourceUpdatedEntity : resourceUpdatedEntities) {
      logger.info("the resource entity is");
      logger.info(resourceUpdatedEntity.toString());
      resourceDao.update(resourceUpdatedEntity);
    }
    // initialize role data
    var existingRoleNames = roleDao.listAllRoleNames();
    List<RoleEntity> roleEntities = new ArrayList<>();
    for (UserRole userRole : UserRole.values()) {
      if (existingRoleNames.contains(userRole.name())) {
        continue;
      }
      RoleEntity roleEntity = RoleEntityBuilder.newBuilder().setName(userRole.toString()).build();
      roleEntities.add(roleEntity);
    }
    if (!roleEntities.isEmpty()) {
      roleDao.batchInsert(roleEntities);
    }
    // assign resource and permissions to role
    for (var role : UserRole.values()) {
      var permissionSet = PermissionDefinition.ROLE_TO_PERMISSION_MAP.get(role);
      var roleId =
          roleDao
              .findByRoleName(role.toString())
              .orElseThrow(ExceptionUtils.notFound("Role with name %s not found", role.toString()))
              .getId();
      Map<String, Set<String>> resourceNameToPermissionsMap =
          PermissionDefinition.getResourcePermissionMapByPermissionSet(permissionSet);
      List<RoleResourceEntity> insertEntities = new ArrayList<>();
      for (Map.Entry<String, Set<String>> entry : resourceNameToPermissionsMap.entrySet()) {
        var resourceId =
            resourceDao
                .getResourceByName(entry.getKey())
                .orElseThrow(
                    ExceptionUtils.notFound("resource with name %s not found", entry.getKey()))
                .getId();
        var roleResourceEntityOptional =
            roleResourceDao.findByRoleIdAndResourceId(roleId, resourceId);
        var newEntity =
            RoleResourceEntityBuilder.newBuilder()
                .setResourceId(resourceId)
                .setRoleId(roleId)
                .permissions(
                    entry.getValue().stream()
                        .map(
                            permission -> {
                              return entry.getKey().trim() + "_" + permission.trim();
                            })
                        .collect(Collectors.toSet()))
                .build();
        if (roleResourceEntityOptional.isEmpty()) {
          insertEntities.add(newEntity);
        } else {
          roleResourceDao.update(newEntity);
        }
      }
      if (!insertEntities.isEmpty()) {
        roleResourceDao.batchInsert(insertEntities);
      }
    }
  }
}
