package com.yichi.onepiece.assistant.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.assistant.service.AiTurn;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns stored messages into the transcript replayed to the model: the last
 * {@value #MAX_TURNS} questions with their tool rounds, long tool output
 * trimmed to bound the prompt, and any tool round that is missing a result
 * dropped (the API rejects a tool call that is not followed by all its results).
 */
final class ConversationReplay {
    static final int MAX_TURNS = 10;
    static final int MAX_TOOL_CONTENT = 4000;
    static final String TRUNCATED = "…(truncated)";

    /** {@code resources} are everything the replayed turns referenced, for link repair. */
    record Replay(List<ObjectNode> messages, List<ResourceLink> resources) {}

    private ConversationReplay() {}

    static Replay build(List<AiTurn> stored, ObjectMapper objectMapper) {
        List<AiTurn> window = lastTurns(stored);
        List<ObjectNode> messages = new ArrayList<>();
        List<ResourceLink> resources = new ArrayList<>();
        int index = 0;
        while (index < window.size()) {
            AiTurn turn = window.get(index);
            if (AiTurn.USER.equals(turn.role())
                    || (AiTurn.ASSISTANT.equals(turn.role()) && !turn.hasToolCalls())) {
                messages.add(Transcript.message(objectMapper, turn.role(), turn.content()));
                resources.addAll(turn.resources());
                index++;
            } else if (AiTurn.ASSISTANT.equals(turn.role())) {
                int next = index + 1;
                Map<String, AiTurn> results = new LinkedHashMap<>();
                while (next < window.size() && AiTurn.TOOL.equals(window.get(next).role())) {
                    AiTurn result = window.get(next);
                    if (result.toolCallId() != null) {
                        results.putIfAbsent(result.toolCallId(), result);
                    }
                    next++;
                }
                ArrayNode calls = parseToolCalls(objectMapper, turn.toolCalls());
                Set<String> callIds = callIds(calls);
                if (!callIds.isEmpty() && results.keySet().containsAll(callIds)) {
                    messages.add(Transcript.assistantToolCalls(objectMapper, turn.content(), calls));
                    for (String callId : callIds) {
                        AiTurn result = results.get(callId);
                        messages.add(Transcript.tool(objectMapper, callId, trimToolContent(result.content())));
                        resources.addAll(result.resources());
                    }
                }
                index = next;
            } else {
                // A tool result whose call is not in the window: replaying it alone is invalid.
                index++;
            }
        }
        return new Replay(messages, resources);
    }

    private static List<AiTurn> lastTurns(List<AiTurn> stored) {
        int questions = 0;
        for (int i = stored.size() - 1; i >= 0; i--) {
            if (AiTurn.USER.equals(stored.get(i).role()) && ++questions == MAX_TURNS) {
                return stored.subList(i, stored.size());
            }
        }
        return stored;
    }

    private static ArrayNode parseToolCalls(ObjectMapper objectMapper, String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node instanceof ArrayNode array ? array : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Set<String> callIds(ArrayNode calls) {
        Set<String> ids = new LinkedHashSet<>();
        if (calls != null) {
            for (JsonNode call : calls) {
                String id = call.path("id").asText("");
                if (id.isEmpty()) {
                    return Set.of();
                }
                ids.add(id);
            }
        }
        return ids;
    }

    /** Tool output beyond this is never replayed, so it is also not worth storing. */
    static String trimToolContent(String content) {
        if (content == null) {
            return "";
        }
        return content.length() <= MAX_TOOL_CONTENT ? content : content.substring(0, MAX_TOOL_CONTENT) + TRUNCATED;
    }
}
