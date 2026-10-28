package com.yichi.onepiece.assistant.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.Completion;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.ToolCall;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.Usage;
import org.testng.annotations.Test;

public class DeepSeekStreamAccumulatorTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void streamsAnswerTextAndStopsAtDone() {
        var accumulator = new DeepSeekStreamAccumulator(objectMapper);
        assertThat(accumulator.accept(": keep-alive")).isNull();
        assertThat(accumulator.accept("")).isNull();
        assertThat(accumulator.accept(contentChunk("Hello"))).isEqualTo("Hello");
        assertThat(accumulator.accept(contentChunk(", world"))).isEqualTo(", world");
        assertThat(accumulator.accept(
                "data: {\"choices\":[],\"usage\":{\"prompt_tokens\":12,\"completion_tokens\":3}}")).isNull();
        assertThat(accumulator.isDone()).isFalse();

        accumulator.accept("data: [DONE]");

        assertThat(accumulator.isDone()).isTrue();
        Completion completion = accumulator.result();
        assertThat(completion.content()).isEqualTo("Hello, world");
        assertThat(completion.hasToolCalls()).isFalse();
        assertThat(completion.usage()).isEqualTo(new Usage(12, 3));
    }

    @Test
    public void stitchesParallelToolCallFragmentsByIndex() {
        var accumulator = new DeepSeekStreamAccumulator(objectMapper);
        accumulator.accept(toolChunk(0, "call_a", "search_characters", ""));
        accumulator.accept(toolChunk(0, null, null, "{\"name\":"));
        accumulator.accept(toolChunk(1, "call_b", "search_fruits", "{\"name\":\"Gomu\"}"));
        accumulator.accept(toolChunk(0, null, null, "\"Luffy\"}"));
        accumulator.accept("data: [DONE]");

        assertThat(accumulator.result().toolCalls()).containsExactly(
                new ToolCall("call_a", "search_characters", "{\"name\":\"Luffy\"}"),
                new ToolCall("call_b", "search_fruits", "{\"name\":\"Gomu\"}"));
    }

    @Test
    public void givesAToolCallWithoutAnIdAStableOne() {
        var accumulator = new DeepSeekStreamAccumulator(objectMapper);
        accumulator.accept(toolChunk(0, null, "list_top_characters", "{}"));

        assertThat(accumulator.result().toolCalls()).containsExactly(
                new ToolCall("call_0", "list_top_characters", "{}"));
    }

    @Test
    public void surfacesAnErrorChunk() {
        var accumulator = new DeepSeekStreamAccumulator(objectMapper);

        assertThatThrownBy(() -> accumulator.accept("data: {\"error\":{\"message\":\"Insufficient Balance\"}}"))
                .isInstanceOf(AssistantException.class)
                .hasMessageContaining("Insufficient Balance");
    }

    private String contentChunk(String text) {
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("choices").addObject().putObject("delta").put("content", text);
        return "data: " + root;
    }

    private String toolChunk(int index, String id, String name, String arguments) {
        ObjectNode call = objectMapper.createObjectNode().put("index", index);
        if (id != null) {
            call.put("id", id);
        }
        ObjectNode function = call.putObject("function");
        if (name != null) {
            function.put("name", name);
        }
        function.put("arguments", arguments);
        ObjectNode root = objectMapper.createObjectNode();
        root.putArray("choices").addObject().putObject("delta").putArray("tool_calls").add(call);
        return "data: " + root;
    }
}
