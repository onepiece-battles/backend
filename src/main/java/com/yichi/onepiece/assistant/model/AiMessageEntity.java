package com.yichi.onepiece.assistant.model;

import com.yichi.infra.support.entity.Entity;

import java.util.UUID;

/**
 * A single turn in an AI thread. Assistant turns store markdown in {@code content}
 * and a JSON array of {type,id,name} links in {@code resources}. Tool rounds are
 * stored too: an assistant turn with a JSON {@code toolCalls} array, then one
 * {@code tool} turn per result keyed by {@code toolCallId}.
 */
@Entity
public interface AiMessageEntity {
    UUID getId();
    UUID getConversationId();
    UUID getUserId();
    String getRole();
    String getContent();
    String getResources();
    String getToolCalls();
    String getToolCallId();
    Long getCreationRecordId();
    Long getModificationRecordId();
    AiMessageEntityBuilder toBuilder();
}
