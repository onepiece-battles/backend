package com.yichi.onepiece.assistant.tools;

import static com.yichi.onepiece.assistant.tools.ToolJson.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.assistant.agent.ToolContext;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.model.I18nText;
import java.util.*;

/** Small projections: no image bytes, navigation neighbours, or unbounded entity graphs. */
final class ToolViews {
  static final ObjectMapper JSON = new ObjectMapper();

  private ToolViews() {}

  static Map<String, Object> fields(Object value, String... names) {
    JsonNode node = value instanceof JsonNode json ? json : JSON.valueToTree(value);
    Map<String, Object> result = new LinkedHashMap<>();
    for (String name : names) result.put(name, node.get(name));
    return result;
  }

  static Map<String, Object> resource(
      Object value, String type, ToolContext context, List<ResourceLink> links) {
    JsonNode node = value instanceof JsonNode json ? json : JSON.valueToTree(value);
    String name =
        context.language().displayName(JSON.convertValue(node.get("name"), I18nText.class));
    String id = node.path("id").asText("");
    if (!id.isBlank()) links.add(new ResourceLink(type, id, name));
    return map("id", id, "name", name);
  }

  static Map<String, Object> character(
      Object value, ToolContext context, List<ResourceLink> links) {
    var view = resource(value, "character", context, links);
    view.putAll(
        fields(
            value,
            "score",
            "bounty",
            "race",
            "battle_style",
            "hakis",
            "maximum_fruit_count",
            "age",
            "height",
            "birth_place",
            "date_of_birth",
            "system_resource"));
    view.put(
        "stats",
        fields(
            value, "attack", "defense", "intelligence", "blood", "speed", "group_gain", "energy"));
    return view;
  }

  static Map<String, Object> fruit(
      Object value, ToolContext context, List<ResourceLink> links, boolean full) {
    JsonNode node = JSON.valueToTree(value);
    var view = resource(node, "fruit", context, links);
    view.putAll(fields(node, "fruit_type", "energy_consume"));
    List<Object> holders = new ArrayList<>();
    for (JsonNode holder : node.path("characters")) {
      var row = resource(holder, "character", context, links);
      row.put("wake_up", holder.get("wake_up"));
      holders.add(row);
    }
    view.put("holders", holders);
    if (full)
      view.put(
          "coefficients",
          fields(
              node,
              "attack_slope",
              "attack_intercept",
              "defense_slope",
              "defense_intercept",
              "blood_slope",
              "blood_intercept",
              "speed_slope",
              "speed_intercept",
              "group_gain_slope",
              "group_gain_intercept",
              "intelligence_slope",
              "intelligence_intercept"));
    return view;
  }

  static Map<String, Object> group(Object value, ToolContext context, List<ResourceLink> links) {
    var view = resource(value, "group", context, links);
    view.putAll(fields(value, "group_category", "father_group_id"));
    return view;
  }

  static Map<String, Object> battle(Object value, ToolContext context, List<ResourceLink> links) {
    JsonNode node = JSON.valueToTree(value);
    var starter = node.path("starter");
    var receiver = node.path("receiver");
    var left = resource(starter, "character", context, links);
    var right = resource(receiver, "character", context, links);
    String label = left.get("name") + " vs " + right.get("name");
    String id = node.path("id").asText();
    links.add(new ResourceLink("battle", id, label));
    return map(
        "id",
        id,
        "name",
        label,
        "type",
        node.get("type"),
        "starter",
        left,
        "receiver",
        right,
        "execution",
        node.get("battle_execution"));
  }

  static Map<String, Object> page(String key, List<?> items, Integer total, int page, int size) {
    return map(
        key,
        items,
        "total_count",
        total,
        "page",
        page,
        "page_size",
        size,
        "has_more",
        total != null && (long) page * size < total);
  }
}
