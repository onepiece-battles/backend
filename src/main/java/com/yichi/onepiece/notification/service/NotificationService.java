package com.yichi.onepiece.notification.service;

import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.notification.mapper.NotificationConverter;
import com.yichi.onepiece.notification.mapper.NotificationDao;
import com.yichi.onepiece.notification.mapper.NotificationEntity;
import com.yichi.onepiece.notification.mapper.NotificationEntityBuilder;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.user.UserHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    private final ActionRecordService actionRecordService;
    private final NotificationDao notificationDao;
    public NotificationService(ActionRecordService actionRecordService, NotificationDao notificationDao) {
        this.actionRecordService = actionRecordService;
        this.notificationDao = notificationDao;
    }

    public CreateNotificationResponse create(CreateNotificationRequest request) {
        ActionRecordEntity actionRecordEntity = actionRecordService.create();
        NotificationEntity notification = NotificationEntityBuilder.newBuilder()
                .setSystemNotification(false)
                .setNotificationSourceId(UserHelper.getCurrentUserId())
                .setNotificationTargetId(request.getReceiverId())
                .setCategory(request.getNotificationType())
                .setCreationRecordId(actionRecordEntity.getId())
                .setModificationRecordId(actionRecordEntity.getId())
                .setContent(request.getContent())
                .setReceived(false)
                .build();
        notificationDao.insert(notification);
        return new CreateNotificationResponse().result(true).notification(new I18nText().enUS("success").zhCN("成功"));
    }

    public CloseNotificationResponse close(CloseNotificationRequest request) {
        ActionRecordEntity actionRecordEntity = actionRecordService.create();
        var notification = notificationDao.findById(request.getNotificationId()).orElseThrow();
        checkNotificationOwner(notification);
        notification = notification
                .toBuilder()
                .setModificationRecordId(actionRecordEntity.getId())
                .setReceived(true).build();
        notificationDao.update(notification);
        return new CloseNotificationResponse().result(true).notification(new I18nText().enUS("success").zhCN("成功"));
    }

    public MarkNotificationReceivedResult markReceived(MarkNotificationReceived request) {
        ActionRecordEntity actionRecordEntity = actionRecordService.create();
        var notification = notificationDao.findById(request.getNotificationId()).orElseThrow();
        checkNotificationOwner(notification);
        notification = notification
                .toBuilder()
                .setModificationRecordId(actionRecordEntity.getId())
                .setReceived(true).build();
        notificationDao.update(notification);
        return new MarkNotificationReceivedResult().result(true).notification(new I18nText().enUS("success").zhCN("成功"));
    }

    public ListNotificationResponse list(ListNotificationRequest request) {
        List<NotificationEntity> notificationEntities = request.getOnlyNew()?
                notificationDao.listNew(UserHelper.getCurrentUserId()):
                notificationDao.list(UserHelper.getCurrentUserId());
        List<Notification> notifications = notificationEntities
                .stream().map(NotificationConverter.INSTANCE::toNotification).toList();
       return new ListNotificationResponse().notifications(notifications).totalSize(notifications.size());
    }

    private void checkNotificationOwner(NotificationEntity notification) {
        if (!UserHelper.hasAdminPermission()
                && !UserHelper.getCurrentUserId().equals(notification.getNotificationTargetId())) {
            throw new PermissionDeniedException("Access Denied");
        }
    }
}
