package com.yichi.onepiece.assistant.importing;

import com.fasterxml.jackson.databind.JsonNode;
import com.yichi.onepiece.assistant.tools.ActionSchemas;
import java.util.ArrayList;
import java.util.List;

/** System prompts of the batch import. Field ranges come from the create schemas themselves. */
final class ImportPrompts {
  private ImportPrompts() {}

  static final String ROSTER =
      """
      You prepare data imports for "One Piece Battles", an app of One Piece characters, devil
      fruits and groups. The user names a scope (a crew, an arc, an organisation, or a list of
      names). Answer with ONE JSON object listing the canonical One Piece entities in that scope:

      {"groups":[{"key":"g1","name":{"zh_CN":"…","en_US":"…"},"group_category":"pirate"}],
       "fruits":[{"key":"f1","name":{"zh_CN":"…","en_US":"…"},"fruit_type":"Paramecia"}],
       "characters":[{"key":"c1","name":{"zh_CN":"…","en_US":"…"},"fruits":["f1"],
                      "group":"g1","position":"captain"}],
       "notes":["…"]}

      Rules:
      - Only entities from the One Piece manga/anime canon. Never invent a person, fruit or group.
      - zh_CN is the usual Simplified Chinese name (as on Chinese One Piece wikis, with · between
        name parts); en_US the usual English name.
      - group_category: pirate, marine, revolution_army, government, under_world or other.
      - fruit_type: Paramecia, Logia, Zoan, Ancient_Zoan, Mythical_Zoan or Artificial_Zoan.
      - position: captain, first_mate, combatant, musician, doctor, cook, helmsman, shipwright,
        navigator, scholar, intern or member.
      - "fruits" lists the keys of the devil fruits a character has eaten; include every such fruit
        in "fruits". "group" is the key of the group the character currently belongs to.
      - Include the groups the scope names; do not add every group a character ever belonged to.
      - At most 30 characters, 15 fruits and 5 groups. If the scope is larger, keep the most
        important ones and say what was left out in notes.
      - notes: short remarks for the person reviewing the draft, in Simplified Chinese.
      """;

  static String detail(String references) {
    return """
        You fill in attributes of One Piece resources for "One Piece Battles". The user sends a
        JSON array of resources (key, type, name and context). Answer with ONE JSON object:

        {"items":[{"key":"c1","data":{…},"uncertain":["bounty"]}]}

        with exactly one entry per requested key. "data" holds the fields below (never "name").

        Character fields:
        %s
        - date_of_birth like "5月5日"; birth_place in Simplified Chinese; height in cm;
          bounty in berries (latest known; leave out if the character has none).
        - maximum_fruit_count is 2 only for a character who ate two fruits, otherwise 1.
        - group_gain: how much the character profits from fighting alongside their crew.

        Fruit fields:
        %s
        - The fruit turns its holder's stat into stat × slope + intercept: a strong combat fruit has
          larger slopes/intercepts for the stats it improves and 1.0 / 0 for the others.
        - energy_consume: energy its holder spends per attack on top of the normal cost.

        Combat stats (attack, defense, blood, speed, intelligence, energy, group_gain) are this
        app's own scale, not canon. Place every character relative to the reference characters
        below (their stats without fruits): stronger than a reference means higher numbers, weaker
        means lower. Avoid a defense so high that equally strong opponents cannot hurt it, or their
        battles never end. Fit new fruits to the reference fruits the same way.

        Facts (age, height, birth_place, date_of_birth, bounty, hakis, race, battle_style) must
        follow canon. If you are not sure of a fact, leave the field out or list it in
        "uncertain". Combat stats are always required.

        References: %s
        """
        .formatted(guide("character"), guide("fruit"), references);
  }

  /** "- attack: integer 10–1500" lines for every create field of a resource type. */
  static String guide(String type) {
    JsonNode properties = ActionSchemas.fields(type, true).path("properties");
    List<String> lines = new ArrayList<>();
    properties
        .fields()
        .forEachRemaining(
            field -> {
              if (field.getKey().equals("name")) return;
              lines.add("- " + field.getKey() + ": " + describe(field.getValue()));
            });
    return String.join("\n", lines);
  }

  private static String describe(JsonNode schema) {
    String type = schema.path("type").asText();
    if (schema.has("enum")) {
      List<String> options = new ArrayList<>();
      schema.get("enum").forEach(option -> options.add(option.asText()));
      return "one of " + String.join(", ", options);
    }
    return switch (type) {
      case "integer", "number" ->
          type
              + " "
              + trim(schema.path("minimum").asDouble())
              + "–"
              + trim(schema.path("maximum").asDouble());
      case "array" ->
          "array of " + describe(schema.path("items")) + " (at most " + schema.path("maxItems").asInt() + ")";
      case "string" -> "text (at most " + schema.path("maxLength").asInt() + " characters)";
      default -> type;
    };
  }

  private static String trim(double value) {
    return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
  }
}
