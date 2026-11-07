package com.yichi.onepiece.file.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A temporary, read-only signed OSS URL for one stored file. The client downloads the bytes
 * directly from OSS; the file bytes never pass through (nor are buffered/base64-encoded in) the
 * app server.
 */
public record FileAccess(
    String url,
    @JsonProperty("file_name") String fileName,
    @JsonProperty("mime_type") String mimeType,
    @JsonProperty("expires_at") long expiresAt) {}
