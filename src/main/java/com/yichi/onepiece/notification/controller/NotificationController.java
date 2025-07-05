package com.yichi.onepiece.notification.controller;


import com.yichi.onepiece.api.NotificationApi;
import com.yichi.onepiece.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationController implements NotificationApi {
    @Override
    public ResponseEntity<CloseNotificationResponse> close(CloseNotificationRequest closeNotificationRequest) {
        return null;
    }

    @Override
    public ResponseEntity<CreateNotificationResponse> create(CreateNotificationRequest createNotificationRequest) {
        return null;
    }

    @Override
    public ResponseEntity<MarkNotificationReceivedResult> update(MarkNotificationReceived markNotificationReceived) {
        return null;
    }
}
