package com.yichi.onepiece.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.ShareResourceRequest;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.UserResourceDao;
import com.yichi.onepiece.user.model.UserResourceEntity;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.testng.annotations.Test;

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
    when(dao.getResourceIdsByUserIdAndResourceType(targetId, ResourceType.CHARACTER))
        .thenReturn(existingIds, List.of(existingId, newId));
    List<UserResourceEntity> inserted = new ArrayList<>();
    doAnswer(invocation -> {
      List<UserResourceEntity> batch = invocation.getArgument(0);
      inserted.addAll(batch);
      return null;
    }).when(dao).batchInsert(anyList());
    var request = new ShareResourceRequest().targetUserId(targetId)
        .resourceType(ResourceType.CHARACTER).resourceIds(List.of(existingId, newId, newId));
    try (var userHelper = mockStatic(UserHelper.class);
        var constants = mockStatic(UserFacingConstants.class)) {
      userHelper.when(UserHelper::getCurrentUserId).thenReturn(senderId);
      constants.when(() -> UserFacingConstants.getConstant(anyString()))
          .thenReturn(new UserFacingConstants.Constant());
      service.shareResource(request);
      service.shareResource(request);
      verify(dao, times(1)).batchInsert(anyList());
      assertThat(inserted).hasSize(1);
      assertThat(inserted.get(0).getResourceId()).isEqualTo(newId);
      assertThat(inserted.get(0).getUserId()).isEqualTo(targetId);
      assertThat(inserted.get(0).getSourceUserId()).isEqualTo(senderId);
      assertThat(existingIds).containsExactly(existingId);
    }
  }
}
