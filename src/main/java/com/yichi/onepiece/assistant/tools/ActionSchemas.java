package com.yichi.onepiece.assistant.tools;

import static com.yichi.onepiece.assistant.tools.ToolJson.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.yichi.onepiece.model.*;
import java.util.*;

/** The write allowlist is also validated on the server, independently of model compliance. */
public final class ActionSchemas {
  static final List<String> OPERATIONS =
      List.of(
          "create_character",
          "update_character",
          "eat_fruit",
          "join_group",
          "create_fruit",
          "update_fruit",
          "create_group",
          "update_group",
          "add_group_members",
          "attach_subgroups",
          "create_battle",
          "start_battle");
  static final Set<String> STATS =
      Set.of("attack", "defense", "blood", "speed", "intelligence", "group_gain");

  private ActionSchemas() {}

  static ObjectNode object() {
    return schema("{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}");
  }

  static ObjectNode string(int max) {
    return schema("{\"type\":\"string\",\"minLength\":1,\"maxLength\":" + max + "}");
  }

  static ObjectNode number(double min, double max, boolean integer) {
    return schema(
        "{\"type\":\""
            + (integer ? "integer" : "number")
            + "\",\"minimum\":"
            + min
            + ",\"maximum\":"
            + max
            + "}");
  }

  static ObjectNode enums(Object[] values) {
    var node = string(100);
    var options = node.putArray("enum");
    for (var v : values) options.add(v.toString());
    return node;
  }

  static ObjectNode array(ObjectNode items, int max) {
    var result = schema("{\"type\":\"array\",\"maxItems\":" + max + ",\"uniqueItems\":true}");
    result.set("items", items);
    return result;
  }

  static void prop(ObjectNode node, String key, JsonNode value) {
    ((ObjectNode) node.get("properties")).set(key, value);
  }

  static void required(ObjectNode node, String... keys) {
    var r = node.putArray("required");
    for (var key : keys) r.add(key);
  }

  static ObjectNode i18n(int max) {
    var node = object();
    prop(node, "zh_CN", string(max));
    prop(node, "en_US", string(max));
    node.put("minProperties", 1);
    return node;
  }

  /**
   * Writes an {@link #i18n} value onto {@code text}, keeping the text of a language it leaves out.
   * Read by hand: the schema's keys are zh_CN / en_US while I18nText's JSON properties are zh-CN /
   * en-US, so convertValue would silently drop the name.
   */
  public static I18nText i18nText(JsonNode value, I18nText text) {
    if (value.hasNonNull("zh_CN")) text.zhCN(value.get("zh_CN").asText());
    if (value.hasNonNull("en_US")) text.enUS(value.get("en_US").asText());
    return text;
  }

  static ObjectNode fields(String resource, boolean create) {
    var node = object();
    prop(node, "name", i18n(100));
    switch (resource) {
      case "character" -> {
        prop(node, "race", enums(Race.values()));
        prop(node, "battle_style", enums(BattleStyle.values()));
        prop(node, "hakis", array(enums(Haki.values()), 3));
        prop(node, "age", number(0, 100000, true));
        prop(node, "height", number(1, 100000, true));
        prop(node, "birth_place", string(200));
        prop(node, "date_of_birth", string(100));
        prop(node, "bounty", number(10, 10000000000L, true));
        prop(node, "maximum_fruit_count", number(1, 2, true));
        prop(node, "group_gain", number(100, 200, true));
        prop(node, "attack", number(10, 1500, true));
        prop(node, "defense", number(10, 1400, true));
        prop(node, "blood", number(100, 10000, true));
        prop(node, "speed", number(10, 200, true));
        prop(node, "intelligence", number(70, 260, true));
        prop(node, "energy", number(10, 50, true));
        if (create)
          required(
              node,
              "name",
              "maximum_fruit_count",
              "group_gain",
              "attack",
              "defense",
              "blood",
              "speed",
              "intelligence",
              "energy",
              "race",
              "battle_style");
      }
      case "fruit" -> {
        prop(node, "fruit_type", enums(FruitType.values()));
        prop(node, "energy_consume", number(0, 100, true));
        String[] stats = {"attack", "defense", "blood", "group_gain", "speed", "intelligence"};
        double[] maxSlopes = {1.5, 1.5, 1.5, 2, 3, 2};
        int[] mins = {-100, -100, -1000, -10, -10, -30}, maxs = {300, 800, 3000, 30, 30, 30};
        for (int i = 0; i < stats.length; i++) {
          prop(node, stats[i] + "_slope", number(.8, maxSlopes[i], false));
          prop(node, stats[i] + "_intercept", number(mins[i], maxs[i], true));
        }
        if (create) {
          var req = node.putArray("required");
          node.get("properties").fieldNames().forEachRemaining(req::add);
        }
      }
      case "group" -> {
        prop(node, "group_category", enums(GroupCategory.values()));
        if (create) required(node, "name", "group_category");
      }
      default -> throw problem("invalid_arguments", "Unknown resource type");
    }
    node.put("minProperties", 1);
    return node;
  }

  static ObjectNode parameters(String operation) {
    var node = object();
    if (operation.startsWith("create_") && !operation.equals("create_battle")) {
      String type = operation.substring(7);
      prop(node, "data", fields(type, true));
      required(node, "data");
    } else if (operation.startsWith("update_")) {
      String type = operation.substring(7);
      prop(node, type, string(200));
      prop(node, "changes", fields(type, false));
      required(node, type, "changes");
    } else
      switch (operation) {
        case "eat_fruit" -> {
          prop(node, "character", string(200));
          prop(node, "fruit", string(200));
          required(node, "character", "fruit");
        }
        case "join_group" -> {
          prop(node, "character", string(200));
          prop(node, "group", string(200));
          prop(node, "position", enums(GroupPosition.values()));
          required(node, "character", "group", "position");
        }
        case "add_group_members" -> {
          prop(node, "group", string(200));
          var chars = array(string(200), 10);
          chars.put("minItems", 1);
          prop(node, "characters", chars);
          required(node, "group", "characters");
        }
        case "attach_subgroups" -> {
          prop(node, "group", string(200));
          var groups = array(string(200), 10);
          groups.put("minItems", 1);
          prop(node, "children", groups);
          required(node, "group", "children");
        }
        case "create_battle" -> {
          prop(node, "starter", string(200));
          prop(node, "receiver", string(200));
          required(node, "starter", "receiver");
        }
        case "start_battle" -> {
          prop(node, "battle", string(200));
          required(node, "battle");
        }
        default -> throw problem("invalid_arguments", "Unknown action");
      }
    return node;
  }

  public static void validate(JsonNode value, JsonNode schema, String path) {
    if (value == null || value.isNull())
      throw problem("invalid_arguments", path + " cannot be null");
    String type = schema.path("type").asText();
    if (type.isEmpty()) return;
    switch (type) {
      case "object" -> {
        if (!value.isObject()) throw problem("invalid_arguments", path + " must be an object");
        if (value.size() < schema.path("minProperties").asInt(0))
          throw problem("invalid_arguments", path + " cannot be empty");
        for (var r : schema.path("required"))
          if (!value.has(r.asText()))
            throw problem("invalid_arguments", path + " requires " + r.asText());
        value
            .fields()
            .forEachRemaining(
                entry -> {
                  var child = schema.path("properties").get(entry.getKey());
                  if (child == null) {
                    if (schema.path("additionalProperties").isBoolean()
                        && !schema.path("additionalProperties").asBoolean())
                      throw problem(
                          "invalid_arguments", "Unsupported field: " + path + "." + entry.getKey());
                  } else validate(entry.getValue(), child, path + "." + entry.getKey());
                });
      }
      case "string" -> {
        if (!value.isTextual()
            || value.asText().trim().length() < schema.path("minLength").asInt(0)
            || value.asText().length() > schema.path("maxLength").asInt(Integer.MAX_VALUE))
          throw problem("invalid_arguments", "Invalid string: " + path);
      }
      case "integer", "number" -> {
        if (!value.isNumber()
            || (type.equals("integer") && !value.isIntegralNumber())
            || !Double.isFinite(value.asDouble())
            || value.asDouble() < schema.path("minimum").asDouble(-Double.MAX_VALUE)
            || value.asDouble() > schema.path("maximum").asDouble(Double.MAX_VALUE))
          throw problem("invalid_arguments", "Number out of range: " + path);
      }
      case "array" -> {
        if (!value.isArray()
            || value.size() < schema.path("minItems").asInt(0)
            || value.size() > schema.path("maxItems").asInt(30))
          throw problem("invalid_arguments", "Invalid array: " + path);
        var seen = new HashSet<JsonNode>();
        for (var entry : value) {
          if (schema.path("uniqueItems").asBoolean() && !seen.add(entry))
            throw problem("invalid_arguments", "Duplicate in " + path);
          validate(entry, schema.get("items"), path + "[]");
        }
      }
      case "boolean" -> {
        if (!value.isBoolean()) throw problem("invalid_arguments", path + " must be boolean");
      }
      default -> throw new IllegalStateException("Unsupported schema type " + type);
    }
    if (schema.has("enum")) {
      boolean found = false;
      for (var option : schema.get("enum")) if (option.equals(value)) found = true;
      if (!found) throw problem("invalid_arguments", "Unsupported value: " + path);
    }
  }
}
