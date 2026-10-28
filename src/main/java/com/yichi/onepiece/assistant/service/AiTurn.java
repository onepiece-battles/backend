package com.yichi.onepiece.assistant.service;

import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import java.util.List;

/**
 * One stored message of an AI thread. Besides the user's questions and the
 * final answers this includes the hidden tool rounds — an assistant message
 * carrying {@code toolCalls}, then one {@code tool} message per result — so
 * follow-up questions can reuse what earlier lookups found.
 */
public record AiTurn(
        String role, String content, String toolCalls, String toolCallId, List<ResourceLink> resources) {
    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";
    public static final String TOOL = "tool";

    public AiTurn {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }

    public static AiTurn user(String content) {
        return new AiTurn(USER, content, null, null, List.of());
    }

    public static AiTurn assistant(String content, List<ResourceLink> resources) {
        return new AiTurn(ASSISTANT, content, null, null, resources);
    }

    public static AiTurn assistantToolCalls(String content, String toolCallsJson) {
        return new AiTurn(ASSISTANT, content, toolCallsJson, null, List.of());
    }

    public static AiTurn tool(String toolCallId, String content, List<ResourceLink> resources) {
        return new AiTurn(TOOL, content, null, toolCallId, resources);
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isBlank();
    }

    /** Shown in the chat: the user's questions and the final answers, never the tool rounds. */
    public boolean visible() {
        return USER.equals(role) || (ASSISTANT.equals(role) && !hasToolCalls());
    }
}
