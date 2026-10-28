package com.yichi.onepiece.assistant.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.ConversationReplay.Replay;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.assistant.service.AiTurn;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

public class ConversationReplayTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void replaysQuestionsAnswersAndCompleteToolRounds() {
        var luffy = new ResourceLink("character", "c1", "Luffy");
        Replay replay = build(List.of(
                AiTurn.user("Who is Luffy?"),
                AiTurn.assistantToolCalls("", calls("call_1")),
                AiTurn.tool("call_1", "{\"name\":\"Luffy\"}", List.of(luffy)),
                AiTurn.assistant("[Luffy](onepiece://character/c1) is a pirate.", List.of(luffy))));

        assertThat(roles(replay)).containsExactly("user", "assistant", "tool", "assistant");
        assertThat(replay.messages().get(1).get("tool_calls").get(0).get("id").asText()).isEqualTo("call_1");
        assertThat(replay.messages().get(2).get("tool_call_id").asText()).isEqualTo("call_1");
        assertThat(replay.resources()).contains(luffy);
    }

    @Test
    public void dropsAToolRoundThatIsMissingAResult() {
        Replay replay = build(List.of(
                AiTurn.user("Compare Luffy and Zoro"),
                AiTurn.assistantToolCalls("", calls("call_1", "call_2")),
                AiTurn.tool("call_1", "{}", List.of()),
                AiTurn.user("Hello?")));

        assertThat(roles(replay)).containsExactly("user", "user");
    }

    @Test
    public void dropsToolResultsWhoseCallIsNotReplayed() {
        assertThat(roles(build(List.of(AiTurn.tool("call_9", "{}", List.of()), AiTurn.user("hi")))))
                .containsExactly("user");
    }

    @Test
    public void keepsOnlyTheLastTenQuestions() {
        List<AiTurn> stored = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            stored.add(AiTurn.user("question " + i));
            stored.add(AiTurn.assistant("answer " + i, List.of()));
        }

        Replay replay = build(stored);

        assertThat(replay.messages()).hasSize(2 * ConversationReplay.MAX_TURNS);
        assertThat(replay.messages().get(0).get("content").asText()).isEqualTo("question 5");
    }

    @Test
    public void trimsLongToolOutput() {
        String huge = "x".repeat(ConversationReplay.MAX_TOOL_CONTENT + 500);

        Replay replay = build(List.of(
                AiTurn.user("q"),
                AiTurn.assistantToolCalls("", calls("call_1")),
                AiTurn.tool("call_1", huge, List.of())));

        String content = replay.messages().get(2).get("content").asText();
        assertThat(content).endsWith(ConversationReplay.TRUNCATED);
        assertThat(content).hasSize(ConversationReplay.MAX_TOOL_CONTENT + ConversationReplay.TRUNCATED.length());
    }

    private Replay build(List<AiTurn> stored) {
        return ConversationReplay.build(stored, objectMapper);
    }

    private static List<String> roles(Replay replay) {
        return replay.messages().stream().map(message -> message.get("role").asText()).toList();
    }

    private String calls(String... ids) {
        ArrayNode array = objectMapper.createArrayNode();
        for (String id : ids) {
            ObjectNode call = array.addObject();
            call.put("id", id);
            call.put("type", "function");
            call.putObject("function").put("name", "search_characters").put("arguments", "{}");
        }
        return array.toString();
    }
}
