package com.yichi.onepiece.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.ShareResourceRequest;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.UserResourceDao;
import com.yichi.onepiece.user.model.UserResourceEntity;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

public class UserResourceSharingTests {
  @Test
  public void insertsOnlyNewDistinctSharesAndRetryIsNoOp() {
    var dao = mock(UserResourceDao.class);
    var service = new UserService(null, null, null, null, null, null, dao, null, null, null, null, null, null);
    var senderId = UUID.randomUUID();
    var targetId = UUID.randomUUID();
    var existingId = UUID.randomUUID();
    var newId = UUID.randomUUID();
    var existingIds = List.of(existingId);
    when(dao.countShareableResources(senderId, ResourceType.CHARACTER, List.of(existingId, newId), false))
        .thenReturn(2);
    when(dao.getResourceIdsByUserIdAndResourceType(targetId, ResourceType.CHARACTER))
        .thenReturn(existingIds, List.of(existingId, newId));
    List<UserResourceEntity> inserted = new ArrayList<>();
    doAnswer(invocation -> {
      List<UserResourceEntity> batch = invocation.getArgument(0);
      inserted.addAll(batch);
      return 1;
    }).when(dao).insertSharesIfAbsent(anyList());
    var request = new ShareResourceRequest().targetUserId(targetId)
        .sourceUserId(UUID.randomUUID())
        .resourceType(ResourceType.CHARACTER).resourceIds(List.of(existingId, newId, newId));
    try (var userHelper = mockStatic(UserHelper.class);
        var constants = mockStatic(UserFacingConstants.class)) {
      userHelper.when(UserHelper::getCurrentUserId).thenReturn(senderId);
      constants.when(() -> UserFacingConstants.getConstant(anyString()))
          .thenReturn(new UserFacingConstants.Constant());
      service.shareResource(request);
      service.shareResource(request);
      verify(dao, times(1)).insertSharesIfAbsent(anyList());
      assertThat(inserted).hasSize(1);
      assertThat(inserted.get(0).getResourceId()).isEqualTo(newId);
      assertThat(inserted.get(0).getUserId()).isEqualTo(targetId);
      assertThat(inserted.get(0).getSourceUserId()).isEqualTo(senderId);
      assertThat(existingIds).containsExactly(existingId);
    }
  }

  @DataProvider
  public Object[][] unauthorizedCounts() {
    return new Object[][] {{0}, {1}};
  }

  @Test(dataProvider = "unauthorizedCounts")
  public void rejectsWholeBatchWhenAnyResourceIsMissingOrUnauthorized(int authorizedCount) {
    var dao = mock(UserResourceDao.class);
    var service = new UserService(null, null, null, null, null, null, dao, null, null, null, null, null, null);
    var senderId = UUID.randomUUID();
    var requestedIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    var request = new ShareResourceRequest().targetUserId(UUID.randomUUID())
        .resourceType(ResourceType.FRUIT).resourceIds(requestedIds);
    when(dao.countShareableResources(senderId, ResourceType.FRUIT, requestedIds, false))
        .thenReturn(authorizedCount);
    try (var userHelper = mockStatic(UserHelper.class)) {
      userHelper.when(UserHelper::getCurrentUserId).thenReturn(senderId);
      assertThatThrownBy(() -> service.shareResource(request)).isInstanceOf(PermissionDeniedException.class);
      verify(dao).countShareableResources(senderId, ResourceType.FRUIT, requestedIds, false);
      verifyNoMoreInteractions(dao);
    }
  }

  @DataProvider
  public Object[][] unsupportedTypes() {
    return new Object[][] {{ResourceType.USER}, {ResourceType.ROLE}, {ResourceType.ISSUE},
        {ResourceType.CHATTING}, {null}};
  }

  @Test(dataProvider = "unsupportedTypes")
  public void rejectsUnsupportedTypesBeforeAnyDatabaseAccess(ResourceType resourceType) {
    var dao = mock(UserResourceDao.class);
    var service = new UserService(null, null, null, null, null, null, dao, null, null, null, null, null, null);
    var request = new ShareResourceRequest().targetUserId(UUID.randomUUID())
        .resourceType(resourceType).resourceIds(List.of(UUID.randomUUID()));
    try (var userHelper = mockStatic(UserHelper.class)) {
      userHelper.when(UserHelper::getCurrentUserId).thenReturn(UUID.randomUUID());
      userHelper.when(UserHelper::hasAdminPermission).thenReturn(true);
      assertThatThrownBy(() -> service.shareResource(request)).isInstanceOf(PermissionDeniedException.class);
      verifyNoInteractions(dao);
    }
  }

  @Test
  public void adminStillRequiresEveryResourceToExist() {
    var dao = mock(UserResourceDao.class);
    var service = new UserService(null, null, null, null, null, null, dao, null, null, null, null, null, null);
    var senderId = UUID.randomUUID();
    var requestedIds = List.of(UUID.randomUUID());
    var request = new ShareResourceRequest().targetUserId(UUID.randomUUID())
        .resourceType(ResourceType.GROUP).resourceIds(requestedIds);
    try (var userHelper = mockStatic(UserHelper.class)) {
      userHelper.when(UserHelper::getCurrentUserId).thenReturn(senderId);
      userHelper.when(UserHelper::hasAdminPermission).thenReturn(true);
      assertThatThrownBy(() -> service.shareResource(request)).isInstanceOf(PermissionDeniedException.class);
      verify(dao).countShareableResources(senderId, ResourceType.GROUP, requestedIds, true);
      verifyNoMoreInteractions(dao);
    }
  }

  @DataProvider
  public Object[][] invalidResourceIds() {
    return new Object[][] {{null}, {java.util.Arrays.asList((UUID) null)}};
  }

  @Test(dataProvider = "invalidResourceIds")
  public void rejectsInvalidIdsBeforeAnyDatabaseAccess(List<UUID> resourceIds) {
    var dao = mock(UserResourceDao.class);
    var service = new UserService(null, null, null, null, null, null, dao, null, null, null, null, null, null);
    var request = new ShareResourceRequest().targetUserId(UUID.randomUUID())
        .resourceType(ResourceType.CHARACTER).resourceIds(resourceIds);
    try (var userHelper = mockStatic(UserHelper.class)) {
      userHelper.when(UserHelper::getCurrentUserId).thenReturn(UUID.randomUUID());
      assertThatThrownBy(() -> service.shareResource(request)).isInstanceOf(PermissionDeniedException.class);
      verifyNoInteractions(dao);
    }
  }

  @Test
  public void adminCanShareAnExistingResourceWithoutClaimingOwnership() {
    var dao = mock(UserResourceDao.class);
    var service = new UserService(null, null, null, null, null, null, dao, null, null, null, null, null, null);
    var adminId = UUID.randomUUID();
    var targetId = UUID.randomUUID();
    var requestedIds = List.of(UUID.randomUUID());
    var request = new ShareResourceRequest().targetUserId(targetId)
        .resourceType(ResourceType.GROUP).resourceIds(requestedIds);
    when(dao.countShareableResources(adminId, ResourceType.GROUP, requestedIds, true)).thenReturn(1);
    when(dao.getResourceIdsByUserIdAndResourceType(targetId, ResourceType.GROUP)).thenReturn(List.of());
    try (var userHelper = mockStatic(UserHelper.class);
        var constants = mockStatic(UserFacingConstants.class)) {
      userHelper.when(UserHelper::getCurrentUserId).thenReturn(adminId);
      userHelper.when(UserHelper::hasAdminPermission).thenReturn(true);
      constants.when(() -> UserFacingConstants.getConstant(anyString()))
          .thenReturn(new UserFacingConstants.Constant());
      service.shareResource(request);
      verify(dao).countShareableResources(adminId, ResourceType.GROUP, requestedIds, true);
      verify(dao).insertSharesIfAbsent(anyList());
    }
  }
}
