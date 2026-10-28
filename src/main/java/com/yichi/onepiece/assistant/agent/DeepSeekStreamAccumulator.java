package com.yichi.onepiece.assistant.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.Completion;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.ToolCall;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.Usage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Folds DeepSeek's server-sent-event lines into a {@link Completion}. Answer text
 * is returned line by line as it arrives; tool-call fragments are stitched
 * together by their {@code index} (id and name come first, arguments in pieces).
 */
final class DeepSeekStreamAccumulator {
    private static final String DATA_PREFIX = "data:";

    private final ObjectMapper objectMapper;
    private final StringBuilder content = new StringBuilder();
    private final SortedMap<Integer, ToolCallBuilder> toolCalls = new TreeMap<>();
    private Usage usage = Usage.NONE;
    private boolean done;

    DeepSeekStreamAccumulator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Consumes one SSE line; returns the answer text it added, or null. */
    String accept(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.strip();
        // Blank lines separate events and ":" lines are keep-alive comments.
        if (!trimmed.startsWith(DATA_PREFIX)) {
            return null;
        }
        String data = trimmed.substring(DATA_PREFIX.length()).strip();
        if ("[DONE]".equals(data)) {
            done = true;
            return null;
        }
        JsonNode chunk;
        try {
            chunk = objectMapper.readTree(data);
        } catch (JsonProcessingException e) {
            throw new AssistantException(
                    AssistantException.UPSTREAM_ERROR, "Malformed stream chunk from DeepSeek", e);
        }
        if (chunk.hasNonNull("error")) {
            throw new AssistantException(
                    AssistantException.UPSTREAM_ERROR,
                    "DeepSeek stream error: " + chunk.path("error").path("message").asText("unknown"));
        }
        JsonNode usageNode = chunk.get("usage");
        if (usageNode != null && usageNode.isObject()) {
            usage = new Usage(
                    usageNode.path("prompt_tokens").asInt(), usageNode.path("completion_tokens").asInt());
        }
        JsonNode delta = chunk.path("choices").path(0).path("delta");
        for (JsonNode call : delta.path("tool_calls")) {
            ToolCallBuilder builder =
                    toolCalls.computeIfAbsent(call.path("index").asInt(0), ignored -> new ToolCallBuilder());
            if (call.hasNonNull("id") && !call.get("id").asText().isEmpty()) {
                builder.id = call.get("id").asText();
            }
            JsonNode function = call.path("function");
            if (function.hasNonNull("name")) {
                builder.name.append(function.get("name").asText());
            }
            if (function.hasNonNull("arguments")) {
                builder.arguments.append(function.get("arguments").asText());
            }
        }
        JsonNode text = delta.get("content");
        if (text != null && text.isTextual() && !text.asText().isEmpty()) {
            content.append(text.asText());
            return text.asText();
        }
        return null;
    }

    boolean isDone() {
        return done;
    }

    Completion result() {
        List<ToolCall> calls = new ArrayList<>();
        for (Map.Entry<Integer, ToolCallBuilder> entry : toolCalls.entrySet()) {
            ToolCallBuilder builder = entry.getValue();
            // Tool results must echo the call id, so never leave it empty.
            String id = builder.id == null ? "call_" + entry.getKey() : builder.id;
            calls.add(new ToolCall(id, builder.name.toString(), builder.arguments.toString()));
        }
        return new Completion(content.toString(), calls, usage);
    }

    private static final class ToolCallBuilder {
        private String id;
        private final StringBuilder name = new StringBuilder();
        private final StringBuilder arguments = new StringBuilder();
    }
}
