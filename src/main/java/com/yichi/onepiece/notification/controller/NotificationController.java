package com.yichi.onepiece.notification.controller;


import com.yichi.onepiece.api.NotificationApi;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import java.util.Set;

@Controller
public class NotificationController implements NotificationApi {
    private final IamService iamService;
    private final NotificationService notificationService;

    public NotificationController(IamService iamService, NotificationService notificationService) {
        this.iamService = iamService;
        this.notificationService = notificationService;
    }

    @Override
    public ResponseEntity<CloseNotificationResponse> close(CloseNotificationRequest closeNotificationRequest) {
        iamService.checkPermissionOfCurrentAccount(
                ResourceType.CHATTING, Set.of(Permission.UPDATE));
        var response = notificationService.close(closeNotificationRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CreateNotificationResponse> create(CreateNotificationRequest createNotificationRequest) {
        iamService.checkPermissionOfCurrentAccount(
        ResourceType.CHATTING, Set.of(Permission.CREATE));
        var response = notificationService.create(createNotificationRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ListNotificationResponse> listNotification(ListNotificationRequest listNotificationRequest) {
        iamService.checkPermissionOfCurrentAccount(
                ResourceType.CHATTING, Set.of(Permission.LIST));
        var response = notificationService.list(listNotificationRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<MarkNotificationReceivedResult> markReceived(MarkNotificationReceived markNotificationReceived) {
        iamService.checkPermissionOfCurrentAccount(
                ResourceType.CHATTING, Set.of(Permission.UPDATE));
        var response = notificationService.markReceived(markNotificationReceived);
        return ResponseEntity.ok(response);
    }
}
