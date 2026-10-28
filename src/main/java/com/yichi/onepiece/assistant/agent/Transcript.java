package com.yichi.onepiece.assistant.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.ToolCall;
import java.util.List;

/** Builders for the OpenAI-style messages that make up a model transcript. */
final class Transcript {
    private Transcript() {}

    static ObjectNode message(ObjectMapper objectMapper, String role, String content) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", role);
        message.put("content", content == null ? "" : content);
        return message;
    }

    static ObjectNode assistantToolCalls(ObjectMapper objectMapper, String content, ArrayNode toolCalls) {
        ObjectNode message = message(objectMapper, "assistant", content);
        message.set("tool_calls", toolCalls);
        return message;
    }

    static ObjectNode tool(ObjectMapper objectMapper, String toolCallId, String content) {
        ObjectNode message = message(objectMapper, "tool", content);
        message.put("tool_call_id", toolCallId);
        return message;
    }

    static ArrayNode toolCalls(ObjectMapper objectMapper, List<ToolCall> calls) {
        ArrayNode array = objectMapper.createArrayNode();
        for (ToolCall call : calls) {
            ObjectNode node = array.addObject();
            node.put("id", call.id());
            node.put("type", "function");
            ObjectNode function = node.putObject("function");
            function.put("name", call.name());
            // Replayed calls must carry valid JSON arguments, even when the model sent none.
            String arguments = call.arguments();
            function.put("arguments", arguments == null || arguments.isBlank() ? "{}" : arguments);
        }
        return array;
    }
}
