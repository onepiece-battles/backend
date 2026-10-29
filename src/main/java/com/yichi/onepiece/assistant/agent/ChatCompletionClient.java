package com.yichi.onepiece.assistant.agent;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.function.Consumer;

/** One OpenAI-compatible chat-completion round trip (DeepSeek in production). */
public interface ChatCompletionClient {

    /**
     * Runs one completion. Answer text is handed to {@code onContentDelta} as it
     * streams in; tool calls are assembled and returned once complete.
     */
    Completion complete(CompletionRequest request, Consumer<String> onContentDelta);

    /**
     * {@code tools} is null when the model must answer without calling tools;
     * {@code jsonOutput} asks for a single JSON object as the answer.
     */
    record CompletionRequest(
            String baseUrl,
            String apiKey,
            String model,
            List<ObjectNode> messages,
            ArrayNode tools,
            double temperature,
            int maxTokens,
            boolean jsonOutput) {

        public CompletionRequest(String baseUrl, String apiKey, String model, List<ObjectNode> messages,
                ArrayNode tools, double temperature, int maxTokens) {
            this(baseUrl, apiKey, model, messages, tools, temperature, maxTokens, false);
        }
    }

    record ToolCall(String id, String name, String arguments) {}

    record Usage(int promptTokens, int completionTokens) {
        public static final Usage NONE = new Usage(0, 0);

        public Usage plus(Usage other) {
            return other == null
                    ? this
                    : new Usage(promptTokens + other.promptTokens, completionTokens + other.completionTokens);
        }
    }

    record Completion(String content, List<ToolCall> toolCalls, Usage usage) {
        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }
}
