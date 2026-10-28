package com.yichi.onepiece.image.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Temporary, read-only URLs. The persisted image_uri remains unchanged. */
public record ImageAccess(
    String url,
    @JsonProperty("original_url") String originalUrl,
    @JsonProperty("expires_at") long expiresAt) {}
