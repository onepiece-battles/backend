package com.yichi.onepiece.assistant.model;

import com.yichi.infra.support.entity.Entity;

import java.util.UUID;

/** One running AI chat thread, owned by a single user. */
@Entity
public interface AiConversationEntity {
    UUID getId();
    UUID getUserId();
    Long getCreationRecordId();
    Long getModificationRecordId();
    AiConversationEntityBuilder toBuilder();
}
