package com.yichi.onepiece.assistant.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.ToolResult;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Helpers shared by the tool providers: schemas, argument access and result encoding. */
final class ToolJson {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolJson() {}

    static ObjectNode schema(String json) {
        try {
            return (ObjectNode) MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid tool parameter schema", e);
        }
    }

    static String text(JsonNode arguments, String field) {
        JsonNode value = arguments == null ? null : arguments.get(field);
        return value == null || value.isNull() ? "" : value.asText().trim();
    }

    static int integer(JsonNode arguments, String field, int fallback) {
        JsonNode value = arguments == null ? null : arguments.get(field);
        return value != null && value.canConvertToInt() ? value.asInt() : fallback;
    }

    /** An insertion-ordered map that, unlike {@code Map.of}, allows null values. */
    static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    static ResourceLink link(String type, UUID id, String name) {
        return new ResourceLink(type, id == null ? "" : id.toString(), name);
    }

    static ToolResult result(Object payload, List<ResourceLink> links) {
        try {
            return new ToolResult(MAPPER.writeValueAsString(payload), links);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not encode a tool result", e);
        }
    }

    static <T> List<T> nonNull(List<T> list) {
        return list == null ? List.of() : list;
    }
}
