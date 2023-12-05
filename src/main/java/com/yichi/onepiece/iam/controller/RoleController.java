package com.yichi.onepiece.iam.controller;

import com.yichi.onepiece.api.RoleApi;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.iam.service.RoleService;
import com.yichi.onepiece.model.AddRoleRequest;
import com.yichi.onepiece.model.AddRoleResponse;
import com.yichi.onepiece.model.AssignResourcePermissionsToRoleRequest;
import com.yichi.onepiece.model.AssignResourcePermissionsToRoleResponse;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
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
        ResourceType.ROLE, Set.of(Permission.UPDATE));
    var response = roleService.addRole(addRoleRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AssignResourcePermissionsToRoleResponse> assignResourceToRole(
      AssignResourcePermissionsToRoleRequest assignResourceToRoleRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.ROLE, Set.of(Permission.UPDATE));
    var response =
        roleService.assignResourceToRole(assignResourceToRoleRequest);
    return ResponseEntity.ok(response);
  }
}
