package com.yichi.onepiece.assistant.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** One function the model may call: its JSON-schema parameters and how to run it. */
public record AssistantTool(
        String name, String description, ObjectNode parameters, Handler handler, Labeler labeler) {

    /** Runs the tool for the current user. It may throw; the agent reports the failure to the model. */
    @FunctionalInterface
    public interface Handler {
        ToolResult execute(JsonNode arguments, ToolContext context);
    }

    /** A short progress line shown while the tool runs, e.g. "正在查询角色「路飞」的资料". */
    @FunctionalInterface
    public interface Labeler {
        String label(JsonNode arguments, AssistantLanguage language);
    }
}
