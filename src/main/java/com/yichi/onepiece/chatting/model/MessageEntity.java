package com.yichi.onepiece.chatting.model;

import com.yichi.infra.support.entity.Entity;

import java.util.UUID;

@Entity
public interface MessageEntity {
    UUID getId();
    UUID getUserId();
    UUID getConversationId();
    UUID getPreMessageId();
    String getContent();
    Boolean getRetracted();
    String getImageContentUri();
    String getVoiceContentUri();
    Long getCreationRecordId();
    Long getModificationRecordId();
    MessageEntityBuilder toBuilder();
}
