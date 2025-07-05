package com.yichi.onepiece.chatting.model;

import com.yichi.infra.support.entity.Entity;

import java.util.UUID;

@Entity
public interface UserConversationEntity {
    UUID getId();
    UUID getConversationId();
    UUID getUserId();

}
