package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.yichi.onepiece.assistant.agent.AssistantLanguage;
import com.yichi.onepiece.model.I18nText;
import java.util.*;

/**
 * The reply to a revision, worked out from the data rather than the model's claims: which items
 * changed and in which fields, what was added or removed, and which facts no source verifies, so
 * the reviewer sees at once that nothing else was touched.
 */
final class ImportRevisionSummary {
  static final int MAX_ITEMS = 8;
  private static final Map<String, String> LABELS =
      Map.ofEntries(
          Map.entry("name", "名字"),
          Map.entry("bounty", "悬赏"),
          Map.entry("age", "年龄"),
          Map.entry("height", "身高"),
          Map.entry("birth_place", "出生地"),
          Map.entry("date_of_birth", "生日"),
          Map.entry("attack", "攻击"),
          Map.entry("defense", "防御"),
          Map.entry("blood", "血量"),
          Map.entry("speed", "速度"),
          Map.entry("intelligence", "智力"),
          Map.entry("energy", "精力"),
          Map.entry("group_gain", "团队加成"),
          Map.entry("race", "种族"),
          Map.entry("battle_style", "战斗风格"),
          Map.entry("hakis", "霸气"),
          Map.entry("maximum_fruit_count", "果实上限"),
          Map.entry("fruit_type", "果实类型"),
          Map.entry("energy_consume", "精力消耗"),
          Map.entry("group_category", "团体类型"),
          Map.entry("fruits", "果实"),
          Map.entry("group", "团体"),
          Map.entry("position", "职位"));

  private ImportRevisionSummary() {}

  /**
   * The reply, or null when nothing changed and nothing needs saying. {@code unverified} lists, by
   * key, the facts that were looked up again and stay blank; {@code portraits} the characters
   * whose portrait the page looks up again.
   */
  static String reply(
      List<ImportItem> before,
      List<ImportItem> after,
      Map<String, List<String>> unverified,
      List<String> portraits,
      AssistantLanguage language) {
    var previous = new HashMap<String, ImportItem>();
    before.forEach(i -> previous.put(i.key(), i));
    var kept = new HashSet<String>();
    after.forEach(i -> kept.add(i.key()));
    String comma = language.pick("、", ", ");
    var modified = new ArrayList<String>();
    var added = new ArrayList<String>();
    var removed = new ArrayList<String>();
    var blank = new ArrayList<String>();
    var pictures = new ArrayList<String>();
    for (var item : after) {
      if (portraits.contains(item.key())) pictures.add(name(item, language));
      var old = previous.get(item.key());
      if (old == null) {
        added.add(name(item, language));
        continue;
      }
      var fields = changedFields(old, item);
      var labels = new ArrayList<String>();
      for (String field : fields)
        labels.add(
            label(field, language)
                + (has(old, field) && !has(item, field) ? language.pick("（留空）", " (blank)") : ""));
      String who =
          fields.contains("name")
              ? name(old, language) + " → " + name(item, language)
              : name(item, language);
      if (!labels.isEmpty()) modified.add(who + language.pick("：", ": ") + String.join(comma, labels));
      var still =
          unverified.getOrDefault(item.key(), List.of()).stream()
              .filter(f -> !fields.contains(f))
              .map(f -> label(f, language))
              .toList();
      if (!still.isEmpty()) blank.add(name(item, language) + " " + String.join(comma, still));
    }
    for (var item : before) if (!kept.contains(item.key())) removed.add(name(item, language));
    if (modified.isEmpty()
        && added.isEmpty()
        && removed.isEmpty()
        && blank.isEmpty()
        && pictures.isEmpty()) return null;

    var parts = new ArrayList<String>();
    String semicolon = language.pick("；", "; ");
    if (!modified.isEmpty())
      parts.add(language.pick("已修改 ", "Changed ") + list(modified, semicolon, language));
    if (!added.isEmpty()) parts.add(language.pick("已新增 ", "Added ") + list(added, comma, language));
    if (!removed.isEmpty())
      parts.add(language.pick("已移除 ", "Removed ") + list(removed, comma, language));
    if (!pictures.isEmpty())
      parts.add(
          language.pick("重新查找头像：", "Looking for a new portrait: ")
              + list(pictures, comma, language));
    if (!blank.isEmpty())
      parts.add(
          language.pick("没有可核实的来源，保持留空：", "No source verifies these, so they stay blank: ")
              + list(blank, semicolon, language));
    return language.pick(
        String.join("。", parts) + "。其他内容未改动。",
        String.join(". ", parts) + ". Everything else is unchanged.");
  }

  /** The fields whose values differ, relations included, in the order the item lists them. */
  static Set<String> changedFields(ImportItem before, ImportItem after) {
    var empty = JsonNodeFactory.instance.objectNode();
    JsonNode a = before.data() == null ? empty : before.data();
    JsonNode b = after.data() == null ? empty : after.data();
    var keys = new LinkedHashSet<String>();
    keys.add("name");
    a.fieldNames().forEachRemaining(keys::add);
    b.fieldNames().forEachRemaining(keys::add);
    var result = new LinkedHashSet<String>();
    for (String key : keys) if (!Objects.equals(a.get(key), b.get(key))) result.add(key);
    if (!Objects.equals(list(before.fruits()), list(after.fruits()))) result.add("fruits");
    if (!Objects.equals(before.group(), after.group())) result.add("group");
    if (!Objects.equals(before.position(), after.position())) result.add("position");
    return result;
  }

  static String label(String field, AssistantLanguage language) {
    String stat = field.replaceFirst("_(slope|intercept)$", "");
    String zh = LABELS.getOrDefault(stat, field);
    if (field.endsWith("_slope")) zh += "系数";
    else if (field.endsWith("_intercept")) zh += "加成";
    return language.pick(zh, field.replace('_', ' '));
  }

  private static boolean has(ImportItem item, String field) {
    return item.data() != null && item.data().has(field);
  }

  private static List<String> list(List<String> values) {
    return values == null ? List.of() : values;
  }

  private static String list(List<String> values, String separator, AssistantLanguage language) {
    if (values.size() <= MAX_ITEMS) return String.join(separator, values);
    return String.join(separator, values.subList(0, MAX_ITEMS))
        + language.pick(
            " 等 " + values.size() + " 项", " and " + (values.size() - MAX_ITEMS) + " more");
  }

  private static String name(ImportItem item, AssistantLanguage language) {
    JsonNode name = item.data() == null ? null : item.data().get("name");
    if (name == null) return item.key();
    String zh = name.path("zh_CN").asText("").trim(), en = name.path("en_US").asText("").trim();
    if (zh.isEmpty() && en.isEmpty()) return item.key();
    return language.displayName(
        new I18nText().zhCN(zh.isEmpty() ? null : zh).enUS(en.isEmpty() ? null : en));
  }
}
