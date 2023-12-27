package com.yichi.onepiece.group.controller;

import com.yichi.onepiece.api.GroupApi;
import com.yichi.onepiece.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class GroupController implements GroupApi {
  @Override
  public ResponseEntity<CreateGroupResponse> createGroup(CreateGroupRequest createGroupRequest) {
    return null;
  }

  @Override
  public ResponseEntity<GroupFullView> getGroupFullView(GetGroupRequest getGroupRequest) {
    return null;
  }

  @Override
  public ResponseEntity<ListGroupResponse> listGroups(ListGroupRequest listGroupRequest) {
    return null;
  }
}
