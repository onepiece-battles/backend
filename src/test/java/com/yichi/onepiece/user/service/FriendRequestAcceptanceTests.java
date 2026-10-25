package com.yichi.onepiece.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.model.AcceptFriendRequest;
import com.yichi.onepiece.model.CreateNotificationRequest;
import com.yichi.onepiece.model.NotificationType;
import com.yichi.onepiece.notification.service.NotificationService;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.FriendshipDao;
import com.yichi.onepiece.user.model.UserDao;
import com.yichi.onepiece.user.model.UserEntity;
import java.util.Optional;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

public class FriendRequestAcceptanceTests {
  @Test
  public void acceptsIncomingPendingRequestsOnceAndNotifiesApplicantOnce() {
    var userDao = mock(UserDao.class);
    var friendshipDao = mock(FriendshipDao.class);
    var notifications = mock(NotificationService.class);
    var service = new UserService(userDao, friendshipDao, null, null, null, null, null, null,
        null, null, notifications, null, null);
    var recipientId = UUID.randomUUID();
    var applicantId = UUID.randomUUID();
    var recipient = mock(UserEntity.class);
    when(recipient.getNickname()).thenReturn("Recipient");
    when(userDao.findById(recipientId)).thenReturn(Optional.of(recipient));
    when(friendshipDao.acceptPendingRequests(applicantId, recipientId)).thenReturn(2, 0);
    var request = new AcceptFriendRequest().friendId(applicantId);
    try (var userHelper = mockStatic(UserHelper.class)) {
      userHelper.when(UserHelper::getCurrentUserId).thenReturn(recipientId);
      assertThat(service.acceptFriend(request).getResult()).isTrue();
      assertThatThrownBy(() -> service.acceptFriend(request)).isInstanceOf(PermissionDeniedException.class);
      var notification = ArgumentCaptor.forClass(CreateNotificationRequest.class);
      verify(notifications).create(notification.capture());
      assertThat(notification.getValue().getReceiverId()).isEqualTo(applicantId);
      assertThat(notification.getValue().getCreatorId()).isEqualTo(recipientId);
      assertThat(notification.getValue().getNotificationType()).isEqualTo(NotificationType.FRIEND_ACCEPTED);
      verify(userDao).findById(recipientId);
      verify(friendshipDao, times(2)).acceptPendingRequests(applicantId, recipientId);
      verifyNoMoreInteractions(userDao, friendshipDao, notifications);
    }
  }

  @Test
  public void rejectsWhenNoIncomingPendingRequestWasUpdated() {
    var userDao = mock(UserDao.class);
    var friendshipDao = mock(FriendshipDao.class);
    var notifications = mock(NotificationService.class);
    var service = new UserService(userDao, friendshipDao, null, null, null, null, null, null,
        null, null, notifications, null, null);
    var currentUserId = UUID.randomUUID();
    var otherUserId = UUID.randomUUID();
    try (var userHelper = mockStatic(UserHelper.class)) {
      userHelper.when(UserHelper::getCurrentUserId).thenReturn(currentUserId);
      assertThatThrownBy(() -> service.acceptFriend(new AcceptFriendRequest().friendId(otherUserId)))
          .isInstanceOf(PermissionDeniedException.class);
      verify(friendshipDao).acceptPendingRequests(otherUserId, currentUserId);
      verifyNoInteractions(userDao, notifications);
    }
  }

  @Test
  public void rejectsSelfAcceptanceWithoutDatabaseWrites() {
    var userDao = mock(UserDao.class);
    var friendshipDao = mock(FriendshipDao.class);
    var notifications = mock(NotificationService.class);
    var service = new UserService(userDao, friendshipDao, null, null, null, null, null, null,
        null, null, notifications, null, null);
    var currentUserId = UUID.randomUUID();
    try (var userHelper = mockStatic(UserHelper.class)) {
      userHelper.when(UserHelper::getCurrentUserId).thenReturn(currentUserId);
      assertThatThrownBy(() -> service.acceptFriend(new AcceptFriendRequest().friendId(currentUserId)))
          .isInstanceOf(PermissionDeniedException.class);
      verifyNoInteractions(userDao, friendshipDao, notifications);
    }
  }
}
