package com.yichi.onepiece.assistant.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.ToolResult;
import com.yichi.onepiece.assistant.agent.ToolProblem;
import com.yichi.onepiece.assistant.agent.AssistantLanguage;
import com.yichi.onepiece.model.I18nText;
import java.util.ArrayList;
import java.util.function.Function;
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

    static int boundedInt(JsonNode args, String field, int fallback, int min, int max) {
        JsonNode value = args.get(field);
        if (value == null || value.isNull()) return fallback;
        if (!value.isIntegralNumber() || !value.canConvertToInt()
                || value.intValue() < min || value.intValue() > max) {
            throw problem("invalid_arguments", field + " must be an integer from " + min + " to " + max);
        }
        return value.intValue();
    }

    static Boolean bool(JsonNode args, String field) {
        JsonNode value = args.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isBoolean()) throw problem("invalid_arguments", field + " must be boolean");
        return value.booleanValue();
    }

    static String requiredText(JsonNode args, String field) {
        JsonNode value = args.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank() || value.asText().length() > 200) {
            throw problem("invalid_arguments", field + " must be a nonempty string of at most 200 characters");
        }
        return value.asText().trim();
    }

    static <T> T enumValue(JsonNode value, Class<T> type, String field) {
        if (value == null || value.isNull()) return null;
        try {
            if (!value.isTextual()) throw new IllegalArgumentException();
            return MAPPER.convertValue(value, type);
        } catch (IllegalArgumentException e) {
            throw problem("invalid_arguments", "Invalid " + field + "; use the resource schema's enum values");
        }
    }

    static <T> List<T> enumList(JsonNode args, String field, Class<T> type) {
        JsonNode value = args.get(field);
        if (value == null || value.isNull()) return new ArrayList<>();
        if (!value.isArray() || value.size() > 20) throw problem("invalid_arguments", field + " must be an array");
        List<T> values = new ArrayList<>();
        for (JsonNode item : value) {
            T parsed = enumValue(item, type, field);
            if (parsed == null) throw problem("invalid_arguments", field + " cannot contain null");
            values.add(parsed);
        }
        return values;
    }

    static ToolProblem problem(String code, String message) {
        return new ToolProblem(message, result(map("error", code, "message", message), List.of()));
    }

    static <T> java.util.Optional<T> singleMatch(List<T> candidates, String query, String type,
            Function<T, UUID> id, Function<T, I18nText> name, AssistantLanguage language) {
        List<T> exact = candidates.stream().filter(candidate -> {
            I18nText label = name.apply(candidate);
            return label != null && (query.equalsIgnoreCase(label.getZhCN()) || query.equalsIgnoreCase(label.getEnUS()));
        }).toList();
        if (exact.size() == 1) return java.util.Optional.of(exact.get(0));
        if (exact.isEmpty() && candidates.size() == 1) return java.util.Optional.of(candidates.get(0));
        if (candidates.isEmpty()) return java.util.Optional.empty();
        List<ResourceLink> links = candidates.stream().map(candidate -> link(type,
                id.apply(candidate), language.displayName(name.apply(candidate)))).toList();
        throw new ToolProblem("The name matches several resources", result(map("error", "ambiguous_resource",
                "query", query, "candidates", links, "message", "Ask the user to choose a resource ID"), links));
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
