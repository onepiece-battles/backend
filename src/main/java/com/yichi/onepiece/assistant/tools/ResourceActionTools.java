package com.yichi.onepiece.assistant.tools;

import static com.yichi.onepiece.assistant.tools.ResourceQueryTools.*;
import static com.yichi.onepiece.assistant.tools.ToolJson.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.yichi.onepiece.assistant.agent.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class ResourceActionTools implements AssistantToolProvider {
  private final ActionConfirmationService actions;

  public ResourceActionTools(ActionConfirmationService actions) {
    this.actions = actions;
  }

  @Override
  public List<AssistantTool> tools() {
    var result = new ArrayList<AssistantTool>();
    result.add(
        tool(
            "get_resource_action_schema",
            "Read exact create/update field schemas, enum values and numeric limits. Never invent"
                + " required values; ask the user for missing data.",
            schema(
                "{\"type\":\"object\",\"properties\":{\"resource\":{\"type\":\"string\",\"enum\":[\"character\",\"fruit\",\"group\",\"battle\"]}},\"required\":[\"resource\"],\"additionalProperties\":false}"),
            this::schemas,
            "正在查询资源字段与限制",
            "Loading supported resource fields"));
    result.add(
        tool(
            "list_pending_actions",
            "List this user's unexpired, presented action proposals and their confirmation codes."
                + " Cannot execute or confirm them.",
            schema("{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}"),
            (a, c) -> actions.pending(c),
            "正在查询待确认操作",
            "Loading pending actions"));
    for (String operation : ActionSchemas.OPERATIONS)
      result.add(
          tool(
              "prepare_" + operation,
              "Prepare "
                  + operation.replace('_', ' ')
                  + " only. NEVER writes resource data. Returns an exact summary and a user"
                  + " confirmation code. Only a separate real user message can confirm. "
                  + notes(operation),
              ActionSchemas.parameters(operation),
              (a, c) -> actions.prepare(operation, a, c),
              "正在准备操作，等待你确认",
              "Preparing a change for your confirmation"));
    return result;
  }

  private String notes(String operation) {
    return switch (operation) {
      case "update_character" ->
          "changes use base stats named attack/defense/blood/speed/intelligence/group_gain;"
              + " untouched fields and avatars are preserved.";
      case "create_character", "create_fruit" ->
          "Get the schema and request missing stats/coefficients from the user; do not invent"
              + " them.";
      case "join_group", "add_group_members" ->
          "Moves current membership, retaining previous crew as history; character ownership is"
              + " required.";
      case "attach_subgroups" ->
          "Moves children to a group-category parent; rejects cycles and requires ownership of all"
              + " affected groups.";
      case "create_battle" ->
          "Creates a waiting battle only. Starting it is a separate confirmed operation.";
      case "start_battle" ->
          "Starts an existing CREATED battle asynchronously and generates a report; may change"
              + " participant scores.";
      default -> "";
    };
  }

  private ToolResult schemas(JsonNode a, ToolContext c) {
    String resource = requiredText(a, "resource");
    if (!Set.of("character", "fruit", "group", "battle").contains(resource))
      throw problem("invalid_arguments", "Unknown resource");
    if (resource.equals("battle"))
      return result(
          map(
              "create",
              ActionSchemas.parameters("create_battle"),
              "start",
              ActionSchemas.parameters("start_battle"),
              "confirmation_required",
              true),
          List.of());
    return result(
        map(
            "create",
            ActionSchemas.fields(resource, true),
            "update_changes",
            ActionSchemas.fields(resource, false),
            "confirmation_required",
            true,
            "note",
            "Only supplied fields are changed; localized name updates preserve the other language."
                + " IDs, ownership, images and server metadata cannot be set by the model."),
        List.of());
  }
}
