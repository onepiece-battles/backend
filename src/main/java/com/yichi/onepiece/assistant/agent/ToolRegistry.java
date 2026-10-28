package com.yichi.onepiece.assistant.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Every tool the model can call, collected from the {@link AssistantToolProvider} beans. */
@Component
public class ToolRegistry {
    private final Map<String, AssistantTool> tools = new LinkedHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ToolRegistry(List<AssistantToolProvider> providers) {
        for (AssistantToolProvider provider : providers) {
            for (AssistantTool tool : provider.tools()) {
                if (tools.putIfAbsent(tool.name(), tool) != null) {
                    throw new IllegalStateException("Duplicate assistant tool: " + tool.name());
                }
            }
        }
    }

    public Optional<AssistantTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Collection<AssistantTool> all() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /** The OpenAI / DeepSeek {@code tools} array. */
    public ArrayNode definitions() {
        ArrayNode definitions = objectMapper.createArrayNode();
        for (AssistantTool tool : tools.values()) {
            ObjectNode function = objectMapper.createObjectNode();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.set("parameters", tool.parameters());
            ObjectNode definition = definitions.addObject();
            definition.put("type", "function");
            definition.set("function", function);
        }
        return definitions;
    }
}
