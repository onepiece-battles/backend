package com.yichi.onepiece.group.controller;

import com.yichi.onepiece.api.GroupApi;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class GroupController implements GroupApi {
  private final IamService iamService;
  private final GroupService groupService;

  public GroupController(IamService iamService, GroupService groupService) {
    this.iamService = iamService;
    this.groupService = groupService;
  }

  @Override
  public ResponseEntity<AddCharacterResponse> addCharacter(AddCharacterRequest addCharacterRequest) {
    iamService.checkPermissionOfCurrentAccount(
            ResourceType.GROUP, Set.of(Permission.UPDATE));
    var response = groupService.addCharacter(addCharacterRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<AddGroupResponse> addGroup(AddGroupRequest addGroupRequest) {
    iamService.checkPermissionOfCurrentAccount(
            ResourceType.GROUP, Set.of(Permission.UPDATE));
    var response = groupService.addGroup(addGroupRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<CreateGroupResponse> createGroup(CreateGroupRequest createGroupRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.GROUP, Set.of(Permission.CREATE));
    var response = groupService.create(createGroupRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<GroupFullView> getGroupFullView(GetGroupRequest getGroupRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.GROUP, Set.of(Permission.GET));
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.CHARACTER, Set.of(Permission.GET));
    return ResponseEntity.ok(groupService.getGroupFullView(getGroupRequest));
  }

  @Override
  public ResponseEntity<ListGroupResponse> listGroups(ListGroupRequest listGroupRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.GROUP, Set.of(Permission.LIST));
    return ResponseEntity.ok(groupService.list(listGroupRequest));
  }

  @Override
  public ResponseEntity<UpdateGroupResponse> updateGroup(Group group) {
    iamService.checkPermissionOfCurrentAccount(
            ResourceType.GROUP, Set.of(Permission.UPDATE));
    return ResponseEntity.ok(groupService.update(group));
  }
}
