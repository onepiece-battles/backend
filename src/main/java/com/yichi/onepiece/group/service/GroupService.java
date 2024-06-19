package com.yichi.onepiece.group.service;

import static com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey.GROUP_CREATION_SUCCESS;

import com.yichi.onepiece.character.model.dao.CharacterToGroupDao;
import com.yichi.onepiece.character.model.entity.CharacterToGroupEntity;
import com.yichi.onepiece.character.model.entity.CharacterToGroupEntityBuilder;
import com.yichi.onepiece.group.model.GroupConverter;
import com.yichi.onepiece.group.model.GroupDao;
import com.yichi.onepiece.group.model.GroupEntity;
import com.yichi.onepiece.group.model.GroupEntityBuilder;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
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
  private final CharacterToGroupDao characterToGroupDao;
  private final ActionRecordService actionRecordService;
  private final UserService userService;

  public GroupService(
      GroupDao groupDao,
      CharacterToGroupDao characterToGroupDao,
      ActionRecordService actionRecordService,
      UserService userService) {
    this.groupDao = groupDao;
    this.characterToGroupDao = characterToGroupDao;
    this.actionRecordService = actionRecordService;
    this.userService = userService;
  }

  public CreateGroupResponse create(CreateGroupRequest request) {
    var actionRecord = actionRecordService.create();
    GroupEntity groupEntity =
        GroupEntityBuilder.newBuilder()
            .setName(request.getName())
            .setGroupCategory(request.getGroupCategory())
            .setCreationRecordId(actionRecord.getId())
            .setModificationRecordId(actionRecord.getId())
            .setSystemResource(UserHelper.hasNikaPermission())
            .build();
    groupDao.insert(groupEntity);
    return new CreateGroupResponse()
            .name(request.getName())
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

  public AddCharacterResponse addCharacter(AddCharacterRequest request) {
    // check whether this group can have character
    var group = groupDao.findById(request.getGroupId()).orElseThrow();
    if (GroupCategory.GROUP.equals(group.getGroupCategory())) {
      return new AddCharacterResponse()
              .result(Boolean.FALSE)
              .notification(UserFacingConstants.getConstant(UserFacingConstantsKey.WRONG_MEMBER_TYPE)
                      .getDisplayName());
    }
    UUID groupId = request.getGroupId();
    List<UUID> characters = request.getCharacterIds();
    var characterToGroups = characterToGroupDao.listByCharacterIds(request.getCharacterIds());
    var currentPairs = characterToGroups.stream()
            .filter(characterToGroupEntity -> !characterToGroupEntity.getOnceBelongedTo()).toList();
    // exit current group if present
    for (var item : currentPairs) {
      item = item.toBuilder().setOnceBelongedTo(true).build();
      characterToGroupDao.update(item);
    }
    // update original groups
    var groupMatchEntities = characterToGroups
            .stream()
            .filter(characterToGroupEntity ->
                    characterToGroupEntity.getGroupId().equals(request.getGroupId())
            ).toList();
    var newCharacterGroupPairs = characters.stream()
            .filter(character->!characterToGroups.stream()
                    .map(CharacterToGroupEntity::getCharacterId).toList()
                    .contains(character))
            .toList();

    for (var existingPair: groupMatchEntities) {
      existingPair = existingPair.toBuilder().setOnceBelongedTo(false).build();
      characterToGroupDao.update(existingPair);
    }
    for (var newPair: newCharacterGroupPairs) {
      CharacterToGroupEntity characterToGroupEntity = CharacterToGroupEntityBuilder.newBuilder()
              .setCharacterId(newPair)
              .setGroupId(groupId)
              .position(GroupPosition.MEMBER)
              .onceBelongedTo(false)
              .build();
      characterToGroupDao.insert(characterToGroupEntity);
    }
    return new AddCharacterResponse()
            .result(Boolean.TRUE)
            .notification(UserFacingConstants.getConstant(UserFacingConstantsKey.GROUP_JOIN_SUCCESS)
                    .getDisplayName());
  }

  public AddGroupResponse addGroup(AddGroupRequest request) {
    UUID fatherGroupId = request.getFatherGroupId();
    List<UUID> childGroupIds = request.getChildGroupIds();
    var group = groupDao.findById(fatherGroupId).orElseThrow();
    if (!GroupCategory.GROUP.equals(group.getGroupCategory())) {
      return new AddGroupResponse()
              .result(Boolean.FALSE)
              .notification(UserFacingConstants.getConstant(UserFacingConstantsKey.WRONG_MEMBER_TYPE)
                      .getDisplayName());
    }
    var childGroups = groupDao.listByIds(childGroupIds);

    var updatedGroups = childGroups
            .stream()
            .map(groupEntity -> groupEntity.toBuilder().setFatherGroupId(fatherGroupId).build())
            .toList();
    for (var newGroup: updatedGroups) {
      groupDao.update(newGroup);
    }
    return new AddGroupResponse()
            .result(Boolean.TRUE)
            .notification(UserFacingConstants.getConstant(UserFacingConstantsKey.GROUP_JOIN_SUCCESS)
                    .getDisplayName());
  }



  public GroupFullView getGroupFullView(GetGroupRequest getGroupRequest) {
    var groupFullView = groupDao.getFullViewById(getGroupRequest.getId()).orElseThrow();
    groupFullView = groupFullView.toBuilder()
            .setPreviousGroup(getPreviousGroup(groupFullView.getCreationRecord().getId()))
            .setNextGroup(getNextGroup(groupFullView.getCreationRecord().getId()))
            .build();
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

  public UpdateGroupResponse update(Group group) {
    GroupEntity groupEntity = GroupConverter.INSTANCE.toGroupEntity(group);
    groupDao.update(groupEntity);
    UpdateGroupResponse response = new UpdateGroupResponse();
    return response.result(
            UserFacingConstants.getConstant(UserFacingConstantsKey.UPDATE_SUCCESS)
                    .getDisplayName()).success(Boolean.TRUE);
  }

  public GroupEntity getPreviousGroup(Long currentCreationRecordId) {
    var groupList =  groupDao.getPreviousGroupId(currentCreationRecordId);
    if (groupList.isEmpty()) {
      return null;
    }
    return groupDao.findById(groupList.get(0)).orElseThrow();
  }

  public GroupEntity getNextGroup(Long currentCreationRecordId) {
    var groupList =  groupDao.getNextGroupId(currentCreationRecordId);
    if (groupList.isEmpty()) {
      return null;
    }
    return groupDao.findById(groupList.get(0)).orElseThrow();
  }
}
