package com.yichi.onepiece.assistant.agent;

import java.util.List;

/** A group of related tools; every provider bean is picked up by {@link ToolRegistry}. */
public interface AssistantToolProvider {
    List<AssistantTool> tools();
}
