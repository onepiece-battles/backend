package com.yichi.onepiece.group.service;

import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.GROUP_CREATION_SUCCESS;

import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.group.model.GroupConverter;
import com.yichi.onepiece.group.model.GroupDao;
import com.yichi.onepiece.group.model.GroupEntity;
import com.yichi.onepiece.group.model.GroupEntityBuilder;
import com.yichi.onepiece.iam.service.RoleService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.CreateGroupRequest;
import com.yichi.onepiece.model.CreateGroupResponse;
import com.yichi.onepiece.model.GetGroupRequest;
import com.yichi.onepiece.model.GroupFullView;
import com.yichi.onepiece.model.ListGroupRequest;
import com.yichi.onepiece.model.ListGroupResponse;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.utils.EncryptHelper;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GroupService {
  Logger logger = LoggerFactory.getLogger(GroupService.class);

  private static final ResourceType RESOURCE_TYPE = ResourceType.GROUP;
  private final GroupDao groupDao;
  private final RoleService roleService;
  private final ActionRecordService actionRecordService;
  private final UserService userService;

  public GroupService(
      GroupDao groupDao,
      RoleService roleService,
      ActionRecordService actionRecordService,
      UserService userService) {
    this.groupDao = groupDao;
    this.roleService = roleService;
    this.actionRecordService = actionRecordService;
    this.userService = userService;
  }

  public CreateGroupResponse create(CreateGroupRequest request) {
    var actionRecord = actionRecordService.create();
    var user = UserHelper.getCurrentUserDetail();
    GroupEntity groupEntity =
        GroupEntityBuilder.newBuilder()
            .setName(request.getName())
            .setGroupCategory(request.getGroupCategory())
            .setCreationRecordId(actionRecord.getId())
            .setModificationRecordId(actionRecord.getId())
            .setUserId(user.getId())
            .setRoleId(roleService.getRoleByName(user.getLoginRole()).getId())
            .setSystemResource(UserHelper.hasNikaPermission())
            .build();
    groupDao.insert(groupEntity);
    return new CreateGroupResponse()
        .result(UserFacingConstants.getConstant(GROUP_CREATION_SUCCESS).getDisplayName())
        .id(groupEntity.getId());
  }

  public ListGroupResponse list(ListGroupRequest request) {
    var userId = UserHelper.getCurrentUserId();
    var resourceIds = userService.getResourceIdsByUserIdResourceType(userId, RESOURCE_TYPE);
    var totalSize = groupDao.count(request.getGroupFilter(), userId, resourceIds);
    // pageIndex is 1- based. offset is 0- based.
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return new ListGroupResponse().groups(List.of()).totalSize(totalSize);
    }
    var groupList =
        groupDao
            .list(
                request.getGroupFilter(),
                request.getSortKeys(),
                offset,
                request.getPageSize(),
                userId,
                resourceIds)
            .stream()
            .map(GroupConverter.INSTANCE::toGroupListView)
            .toList();
    return new ListGroupResponse().totalSize(totalSize).groups(groupList);
  }

  public GroupFullView getGroupFullView(GetGroupRequest getGroupRequest) {
    var groupFullView = groupDao.getFullViewById(getGroupRequest.getId()).orElseThrow();
    return GroupConverter.INSTANCE.toGroupFullView(groupFullView);
  }

  public String updateGroupImage(UUID groupId, String image) {
    Byte[] profileByte = ArrayUtils.toObject(EncryptHelper.base64Decode(image));
    var modificationRecord = actionRecordService.create();
    var group = groupDao.findById(groupId).orElseThrow();
    var updatedGroup =
        group.toBuilder()
            .setProfileImage(profileByte)
            .setModificationRecordId(modificationRecord.getId())
            .build();
    groupDao.update(updatedGroup);
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(updatedGroup.getProfileImage()));
  }
}
