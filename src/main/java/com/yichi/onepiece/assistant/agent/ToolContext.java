package com.yichi.onepiece.assistant.agent;

import java.util.UUID;

/**
 * Who a tool runs for. Permission checks still go through the security context
 * the agent propagates onto the tool threads, exactly as for a direct API call.
 */
public record ToolContext(UUID userId, AssistantLanguage language) {}
