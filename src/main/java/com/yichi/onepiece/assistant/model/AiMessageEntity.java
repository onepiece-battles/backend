package com.yichi.onepiece.assistant.model;

import com.yichi.infra.support.entity.Entity;

import java.util.UUID;

/**
 * A single turn in an AI thread. Assistant turns store markdown in {@code content}
 * and a JSON array of {type,id,name} links in {@code resources}.
 */
@Entity
public interface AiMessageEntity {
    UUID getId();
    UUID getConversationId();
    UUID getUserId();
    String getRole();
    String getContent();
    String getResources();
    Long getCreationRecordId();
    Long getModificationRecordId();
    AiMessageEntityBuilder toBuilder();
}
