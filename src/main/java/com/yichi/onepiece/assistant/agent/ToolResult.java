package com.yichi.onepiece.assistant.agent;

import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import java.util.List;

/** JSON handed back to the model, plus the resources it mentioned (for links and chips). */
public record ToolResult(String content, List<ResourceLink> resources) {
    public ToolResult {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }
}
