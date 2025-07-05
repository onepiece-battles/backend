package com.yichi.onepiece.notification.mapper;


import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.NotificationType;

import java.util.UUID;

@Entity
public interface NotificationEntity {
    UUID getId();
    I18nText getContent();
    NotificationType getCategory();
    Long getCreationRecordId();
    Long getModificationRecordId();
    Boolean getReceived();
    Boolean getSystemNotification();
    UUID getNotificationSourceId();
    UUID getNotificationTargetId();
    NotificationEntityBuilder toBuilder();
}
