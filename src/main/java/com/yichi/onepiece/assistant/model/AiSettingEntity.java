package com.yichi.onepiece.assistant.model;

import com.yichi.infra.support.entity.Entity;

import java.util.UUID;

/**
 * The single-row AI provider configuration. Only the NIKA super-admin can write
 * it; the {@code apiKey} never leaves the server.
 */
@Entity
public interface AiSettingEntity {
    UUID getId();
    String getApiKey();
    String getBaseUrl();
    String getModel();
    Long getCreationRecordId();
    Long getModificationRecordId();
    AiSettingEntityBuilder toBuilder();
}
