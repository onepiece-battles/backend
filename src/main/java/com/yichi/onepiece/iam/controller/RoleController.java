package com.yichi.onepiece.iam.controller;

import com.yichi.onepiece.api.RoleApi;
import com.yichi.onepiece.iam.PermissionDefinition.PermissionEnum;
import com.yichi.onepiece.iam.model.RoleEntity;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.iam.service.RoleService;
import com.yichi.onepiece.model.AddRoleRequest;
import com.yichi.onepiece.model.AddRoleResponse;
import com.yichi.onepiece.model.AssignResourceToRoleRequest;
import com.yichi.onepiece.model.AssignResourceToRoleResponse;
import java.util.Set;
import org.springframework.http.ResponseEntity;

public class RoleController implements RoleApi {
  private final RoleService roleService;
  private final IamService iamService;

  public RoleController(RoleService roleService, IamService iamService) {
    this.roleService = roleService;
    this.iamService = iamService;
  }

  @Override
  public ResponseEntity<AddRoleResponse> addRole(AddRoleRequest addRoleRequest) {
    iamService.checkPermissionOfCurrentAccount(
        RoleEntity.ENTITY_TYPE, Set.of(PermissionEnum.ROLE_CREATE));
    var response = roleService.addRole(addRoleRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AssignResourceToRoleResponse> assignResourceToRole(
      AssignResourceToRoleRequest assignResourceToRoleRequest) {
    iamService.checkPermissionOfCurrentAccount(
        RoleEntity.ENTITY_TYPE, Set.of(PermissionEnum.ROLE_UPDATE));
    var response = roleService.assignResourceToRole(assignResourceToRoleRequest);
    return ResponseEntity.ok(response);
  }
}
