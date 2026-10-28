package com.yichi.onepiece.assistant.agent;

/** A failure the client receives as an {@code error} event with a stable {@link #code()}. */
public class AssistantException extends RuntimeException {
    public static final String NOT_CONFIGURED = "not_configured";
    public static final String INVALID_REQUEST = "invalid_request";
    public static final String BUSY = "busy";
    public static final String RATE_LIMITED = "rate_limited";
    public static final String UPSTREAM_ERROR = "upstream_error";
    public static final String TIMEOUT = "timeout";
    public static final String INTERNAL_ERROR = "internal_error";

    private final String code;

    public AssistantException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AssistantException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
