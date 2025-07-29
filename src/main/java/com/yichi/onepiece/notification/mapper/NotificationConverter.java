package com.yichi.onepiece.notification.mapper;


import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.chatting.model.ChatConverter;
import com.yichi.onepiece.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EntityBuilder.class})
public interface NotificationConverter {
    NotificationConverter INSTANCE = Mappers.getMapper(NotificationConverter.class);

    Notification toNotification(NotificationEntity notification);
}
