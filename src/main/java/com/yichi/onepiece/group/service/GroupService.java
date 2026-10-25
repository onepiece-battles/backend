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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  public ListChildCharactersResponse listChildCharacters(ListChildCharactersRequest request) {
   var childEntities =  characterToGroupDao.listCharacterInGroupViewByGroupId(request.getGroupId());
    var childList = childEntities.stream().map(GroupConverter.INSTANCE::toCharacterInGroupView).toList();
    return new ListChildCharactersResponse().childCharacters(childList);
  }

  public ListChildGroupsResponse listChildGroups(ListChildGroupsRequest request) {
    var groupEntities = groupDao.listChildGroups(request.getGroupId());
    var groups = groupEntities.stream().map(GroupConverter.INSTANCE::toGroup).toList();
    return new ListChildGroupsResponse().childGroups(groups);
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
    List<UUID> characterIds = request.getCharacterIds();
    var characterToGroups = characterToGroupDao.listByCharacterIds(request.getCharacterIds());
    var currentPairs = characterToGroups.stream()
            .filter(characterToGroupEntity -> !characterToGroupEntity.getOnceBelongedTo()).toList();
    // exit current group if present
    for (var item : currentPairs) {
      item = item.toBuilder().setOnceBelongedTo(true).build();
      characterToGroupDao.update(item);
    }
    // update original groups
    var groupMatchEntities = characterToGroupDao.listByGroupId(groupId);
    var existingCharactersGroupPairs = groupMatchEntities.stream()
            .filter(groupMatch->characterIds.contains(groupMatch.getCharacterId())).toList();
    var newCharacterIds = characterIds.stream()
            .filter(character->!groupMatchEntities.stream()
                    .map(CharacterToGroupEntity::getCharacterId).toList()
                    .contains(character))
            .toList();

    for (var existingPair: existingCharactersGroupPairs) {
      existingPair = existingPair.toBuilder().setOnceBelongedTo(false).build();
      characterToGroupDao.update(existingPair);
    }
    for (var newPair: newCharacterIds) {
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

  @Transactional
  public AddGroupResponse addGroup(AddGroupRequest request) {
    groupDao.lockHierarchy();
    UUID fatherGroupId = request.getFatherGroupId();
    if (fatherGroupId == null || request.getChildGroupIds() == null
        || request.getChildGroupIds().stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Group IDs must not be null");
    }
    List<UUID> childGroupIds = request.getChildGroupIds().stream().distinct().toList();
    var group = groupDao.findById(fatherGroupId).orElseThrow();
    if (!GroupCategory.GROUP.equals(group.getGroupCategory())) {
      return new AddGroupResponse()
              .result(Boolean.FALSE)
              .notification(UserFacingConstants.getConstant(UserFacingConstantsKey.WRONG_MEMBER_TYPE)
                      .getDisplayName());
    }
    var ancestors = getAncestorIds(fatherGroupId);
    if (childGroupIds.stream().anyMatch(ancestors::contains)) {
      throw new IllegalArgumentException("Group hierarchy must not contain a cycle");
    }
    var childGroups = childGroupIds.isEmpty() ? List.<GroupEntity>of() : groupDao.listByIds(childGroupIds);
    if (childGroups.size() != childGroupIds.size()) {
      throw new IllegalArgumentException("A child group does not exist");
    }

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
    var fatherGroups = new ArrayList<GroupEntity>();
    Set<UUID> visited = new HashSet<>();
    visited.add(getGroupRequest.getId());
    UUID fatherId = groupFullView.getFatherGroupId();
    while (fatherId != null) {
      if (!visited.add(fatherId)) {
        logger.warn("Cycle detected while reading group hierarchy for {}", getGroupRequest.getId());
        break;
      }
      var fatherGroup = groupDao.findById(fatherId).orElseThrow();
      fatherGroups.add(fatherGroup);
      fatherId = fatherGroup.getFatherGroupId();
    }
    groupFullView = groupFullView.toBuilder()
            .setPreviousGroup(getPreviousGroup(groupFullView.getCreationRecord().getId()))
            .setNextGroup(getNextGroup(groupFullView.getCreationRecord().getId()))
            .setFatherGroups(fatherGroups)
            .build();
    return GroupConverter.INSTANCE.toGroupFullView(groupFullView);
  }

  @Transactional
  public void updateGroupImage(UUID groupId, String uri) {
    groupDao.lockHierarchy();
    var modificationRecord = actionRecordService.create();
    var group = groupDao.findById(groupId).orElseThrow();
    var updatedGroup =
        group.toBuilder()
            .setImageUri(uri)
            .setModificationRecordId(modificationRecord.getId())
            .build();
    groupDao.update(updatedGroup);
  }

  @Transactional
  public UpdateGroupResponse update(Group group) {
    groupDao.lockHierarchy();
    if (group.getId() == null || getAncestorIds(group.getFatherGroupId()).contains(group.getId())) {
      throw new IllegalArgumentException("Group hierarchy must not contain a cycle");
    }
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

  public List<GroupEntity> listNoUriGroups() {
    return groupDao.listNoUriGroups();
  }

  private Set<UUID> getAncestorIds(UUID groupId) {
    Set<UUID> ancestors = new HashSet<>();
    UUID currentId = groupId;
    while (currentId != null) {
      if (!ancestors.add(currentId)) {
        throw new IllegalArgumentException("Group hierarchy already contains a cycle");
      }
      currentId = groupDao.findFatherGroupIdForUpdate(currentId);
    }
    return ancestors;
  }
}
