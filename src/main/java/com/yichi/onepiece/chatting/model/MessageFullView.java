package com.yichi.onepiece.chatting.model;

import com.yichi.infra.support.entity.Entity;

@Entity
public interface MessageFullView extends MessageEntity {
    MessageEntity getPreMessage();
}
