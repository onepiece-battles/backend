package com.yichi.onepiece.configuration;

import static com.yichi.onepiece.iam.PermissionDefinition.RESOURCE_PERMISSION_SPLITTER;

import com.yichi.onepiece.iam.PermissionDefinition;
import com.yichi.onepiece.iam.model.*;
import com.yichi.onepiece.model.UserRole;
import java.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PrepareResourceAndPermissionData implements CommandLineRunner {

  Logger logger = LoggerFactory.getLogger(PrepareResourceAndPermissionData.class);
  private final ResourcePermissionDao resourcePermissionDao;
  private final RoleToResourcePermissionDao roleToResourcePermissionDao;
  private final RoleDao roleDao;

  public PrepareResourceAndPermissionData(ResourcePermissionDao resourcePermissionDao,
      RoleToResourcePermissionDao roleToResourcePermissionDao, RoleDao roleDao) {
    this.resourcePermissionDao = resourcePermissionDao;
    this.roleToResourcePermissionDao = roleToResourcePermissionDao;
    this.roleDao = roleDao;
  }

  @Override
  public void run(String... args) {
    // todo(yichi): handle delete occasions
    // initialize resource and permission data
    List<ResourcePermissionEntity> resourcePermissionDbEntities = resourcePermissionDao.listAll();
    var dbRpList = resourcePermissionDbEntities.stream().map(rp-> String.format("%s%s%s", rp.getResourceType(), RESOURCE_PERMISSION_SPLITTER, rp.getPermission())).toList();
    var resourcePermissionList = PermissionDefinition.resourcePermissionList;
    var newRPEntities = resourcePermissionList.stream().map(
        resourceTypePermissionPair -> ResourcePermissionEntityBuilder.newBuilder()
            .setResourceType(resourceTypePermissionPair.getLeft())
            .setPermission(resourceTypePermissionPair.getRight()).build()
    ).filter(rp->
        !dbRpList.contains(
            String.format("%s%s%s", rp.getResourceType(), RESOURCE_PERMISSION_SPLITTER, rp.getPermission()))).toList();
    resourcePermissionDao.batchInsert(newRPEntities);

    // initialize role data
    var existingRoleNames = roleDao.listAllRoleNames();
    List<RoleEntity> roleEntities = new ArrayList<>();
    for (UserRole userRole : UserRole.values()) {
      if (existingRoleNames.contains(userRole)) {
        continue;
      }
      RoleEntity roleEntity = RoleEntityBuilder.newBuilder().setName(userRole).build();
      roleEntities.add(roleEntity);
    }
    if (!roleEntities.isEmpty()) {
      roleDao.batchInsert(roleEntities);
    }

    // delete all rows in  row_to_resource_permission_table
    roleToResourcePermissionDao.clearRoleToResourcePermission();

    // add new row_to_resource_permission_table
    var updatedRoles = roleDao.listAllRoles();
    var updatedResourcePermissions = resourcePermissionDao.listAll();
    logger.info("roles: {}", updatedRoles.toString());
    logger.info("resource_permissions: {}", updatedResourcePermissions.toString());
    for(var role : updatedRoles) {
      initRoleToResourcePermissionTable(role, updatedResourcePermissions);
    }
  }

  private void initRoleToResourcePermissionTable(
      RoleEntity roleEntity,
      List<ResourcePermissionEntity> currentAllResourcePermissions) {
    var roleId = roleEntity.getId();
    var roleToResourcePermissionEntities =
        currentAllResourcePermissions
            .stream()
            .filter(rp-> PermissionDefinition.getPermissionsByRole(roleEntity.getName())
                .contains(Pair.of(rp.getResourceType(), rp.getPermission()))).map(
            ResourcePermissionEntity::getId)
            .map(id-> RoleToResourcePermissionEntityBuilder.newBuilder().setRoleId(roleId)
                .setResourcePermissionId(id).build()).toList();
    if(roleToResourcePermissionEntities.isEmpty()) return;
    roleToResourcePermissionDao.batchInsert(roleToResourcePermissionEntities);
  }
}
