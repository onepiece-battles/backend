package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.AssistantAgent;
import com.yichi.onepiece.assistant.agent.AssistantException;
import com.yichi.onepiece.assistant.agent.AssistantExecutors;
import com.yichi.onepiece.assistant.agent.AssistantLanguage;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient.CompletionRequest;
import com.yichi.onepiece.assistant.service.AiSettingService;
import com.yichi.onepiece.assistant.tools.ActionSchemas;
import com.yichi.onepiece.model.FruitType;
import com.yichi.onepiece.model.GroupCategory;
import com.yichi.onepiece.model.GroupPosition;
import com.yichi.onepiece.model.I18nText;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Drafts a batch of One Piece resources with DeepSeek for a person to review.
 *
 * <ol>
 *   <li>A roster call lists the groups, fruits and characters in the scope and how they relate.
 *   <li>Names that already exist in the app are reused instead of drafted again.
 *   <li>Detail calls (in parallel batches) fill in the create fields, placing combat stats on the
 *       app's own scale by comparison with existing characters and fruits.
 *   <li>Every field is checked against the same schema the assistant's create actions use; what
 *       cannot be repaired is reported per item for the reviewer to fix.
 * </ol>
 *
 * Nothing is written: the reviewed draft is imported by {@link ImportCommitter}.
 */
@Service
public class ImportDrafter {
  private static final Logger logger = LoggerFactory.getLogger(ImportDrafter.class);
  static final int MAX_CHARACTERS = 30;
  static final int MAX_FRUITS = 15;
  static final int MAX_GROUPS = 5;
  static final int BATCH_SIZE = 6;
  static final int REFERENCE_CHARACTERS = 12;
  static final int REFERENCE_FRUITS = 6;
  static final double TEMPERATURE = 0.2;
  static final int MAX_TOKENS = 4000;

  private final AiSettingService settings;
  private final ChatCompletionClient client;
  private final ExistingResources existing;
  private final Executor detailExecutor;
  private final ObjectMapper json = new ObjectMapper();

  @Autowired
  public ImportDrafter(
      AiSettingService settings,
      ChatCompletionClient client,
      ExistingResources existing,
      AssistantExecutors executors) {
    this(settings, client, existing, executors.tools());
  }

  ImportDrafter(
      AiSettingService settings,
      ChatCompletionClient client,
      ExistingResources existing,
      Executor detailExecutor) {
    this.settings = settings;
    this.client = client;
    this.existing = existing;
    this.detailExecutor = detailExecutor;
  }

  private record Settings(String baseUrl, String apiKey, String model) {}

  /** A roster entry before details are filled in. */
  private static final class Entry {
    final String key;
    final String type;
    final I18nText name;
    final ObjectNode rosterData = JsonNodeFactory.instance.objectNode();
    final List<String> fruits = new ArrayList<>();
    String group;
    String position;
    String existingId;

    Entry(String key, String type, I18nText name) {
      this.key = key;
      this.type = type;
      this.name = name;
    }
  }

  public ImportDraft draft(
      String scope, UUID userId, AssistantLanguage language, Consumer<String> progress) {
    if (!settings.isConfigured()) {
      throw new AssistantException(
          AssistantException.NOT_CONFIGURED,
          language.pick("还没有配置 DeepSeek Key。", "No DeepSeek key is configured yet."));
    }
    var s =
        new Settings(
            settings.baseUrl(), settings.apiKey(), AssistantAgent.resolveModel(settings.model()));
    var notes = new ArrayList<String>();

    progress.accept(language.pick("正在梳理名单…", "Listing who and what is in scope…"));
    JsonNode roster = ask(s, ImportPrompts.ROSTER, scope);
    List<Entry> entries = readRoster(roster, notes, language);
    if (entries.isEmpty()) {
      throw new AssistantException(
          AssistantException.INVALID_REQUEST,
          language.pick(
              "没有找到这个范围里的角色、果实或团体，换个说法试试。",
              "Nothing was found in that scope. Try describing it differently."));
    }

    progress.accept(language.pick("正在核对已有数据…", "Checking what already exists…"));
    for (var entry : entries) {
      existing.find(entry.type, entry.name, userId).ifPresent(id -> entry.existingId = id.toString());
    }

    var toDetail =
        entries.stream()
            .filter(e -> e.existingId == null && !e.type.equals(GROUP))
            .toList();
    Map<String, JsonNode> details = new HashMap<>();
    if (!toDetail.isEmpty()) {
      String references = references(userId);
      var batches = new ArrayList<List<Entry>>();
      for (int i = 0; i < toDetail.size(); i += BATCH_SIZE)
        batches.add(toDetail.subList(i, Math.min(i + BATCH_SIZE, toDetail.size())));
      progress.accept(
          language.pick(
              "正在起草 " + toDetail.size() + " 项属性（" + batches.size() + " 批）…",
              "Drafting " + toDetail.size() + " items in " + batches.size() + " batches…"));
      var byKey = new HashMap<String, Entry>();
      entries.forEach(e -> byKey.put(e.key, e));
      var futures =
          batches.stream()
              .map(
                  batch ->
                      CompletableFuture.supplyAsync(
                          () -> ask(s, ImportPrompts.detail(references), detailRequest(batch, byKey)),
                          detailExecutor))
              .toList();
      try {
        for (var future : futures) {
          for (var item : future.join().path("items")) details.put(item.path("key").asText(), item);
        }
      } catch (CompletionException e) {
        futures.forEach(f -> f.cancel(true));
        if (e.getCause() instanceof RuntimeException cause) throw cause;
        throw e;
      }
    }

    progress.accept(language.pick("正在校验草稿…", "Checking the draft…"));
    var items = new ArrayList<ImportItem>();
    for (var entry : entries) items.add(toItem(entry, details.get(entry.key), language));
    return new ImportDraft(items, notes);
  }

  /** One JSON-mode completion; a reply that is not a JSON object is retried once. */
  private JsonNode ask(Settings s, String system, String user) {
    var messages =
        List.of(message("system", system), message("user", user));
    for (int attempt = 1; ; attempt++) {
      var completion =
          client.complete(
              new CompletionRequest(
                  s.baseUrl(), s.apiKey(), s.model(), messages, null, TEMPERATURE, MAX_TOKENS, true),
              text -> {});
      try {
        JsonNode node = json.readTree(completion.content() == null ? "" : completion.content());
        if (node != null && node.isObject()) return node;
      } catch (JsonProcessingException e) {
        logger.warn("import draft reply was not JSON (attempt {})", attempt);
      }
      if (attempt == 2)
        throw new AssistantException(
            AssistantException.UPSTREAM_ERROR, "DeepSeek did not return a JSON object");
    }
  }

  private ObjectNode message(String role, String content) {
    return json.createObjectNode().put("role", role).put("content", content);
  }

  /**
   * Reads the roster with keys reassigned by the server (g1, f1, c1, …): the model's keys only link
   * its own entries. Unknown categories/types/positions are dropped, relations to unlisted keys are
   * dropped, and duplicate names keep their first entry.
   */
  private List<Entry> readRoster(JsonNode roster, List<String> notes, AssistantLanguage language) {
    for (var note : roster.path("notes")) if (note.isTextual()) notes.add(note.asText());
    var entries = new ArrayList<Entry>();
    var groupKeys = new HashMap<String, String>();
    var fruitKeys = new HashMap<String, String>();
    var seen = new HashSet<String>();

    for (var g : limit(roster.path("groups"), MAX_GROUPS, notes, language, "团体", "groups")) {
      var name = name(g.path("name"));
      if (name == null || !seen.add(GROUP + ":" + ExistingResources.normalize(display(name)))) continue;
      var entry = new Entry("g" + (groupKeys.size() + 1), GROUP, name);
      String category = g.path("group_category").asText();
      if (validEnum(category, GroupCategory.values()) && !category.equals("group"))
        entry.rosterData.put("group_category", category);
      groupKeys.put(g.path("key").asText(), entry.key);
      entries.add(entry);
    }
    for (var f : limit(roster.path("fruits"), MAX_FRUITS, notes, language, "果实", "fruits")) {
      var name = name(f.path("name"));
      if (name == null || !seen.add(FRUIT + ":" + ExistingResources.normalize(display(name)))) continue;
      var entry = new Entry("f" + (fruitKeys.size() + 1), FRUIT, name);
      String fruitType = f.path("fruit_type").asText();
      if (validEnum(fruitType, FruitType.values())) entry.rosterData.put("fruit_type", fruitType);
      fruitKeys.put(f.path("key").asText(), entry.key);
      entries.add(entry);
    }
    int characters = 0;
    for (var c :
        limit(roster.path("characters"), MAX_CHARACTERS, notes, language, "角色", "characters")) {
      var name = name(c.path("name"));
      if (name == null || !seen.add(CHARACTER + ":" + ExistingResources.normalize(display(name))))
        continue;
      var entry = new Entry("c" + (++characters), CHARACTER, name);
      for (var fruit : c.path("fruits")) {
        String key = fruitKeys.get(fruit.asText());
        if (key != null && !entry.fruits.contains(key)) entry.fruits.add(key);
      }
      entry.group = groupKeys.get(c.path("group").asText());
      if (entry.group != null) {
        String position = c.path("position").asText();
        entry.position = validEnum(position, GroupPosition.values()) ? position : "member";
      }
      entries.add(entry);
    }
    return entries;
  }

  private static List<JsonNode> limit(
      JsonNode values,
      int max,
      List<String> notes,
      AssistantLanguage language,
      String zh,
      String en) {
    var list = new ArrayList<JsonNode>();
    values.forEach(list::add);
    if (list.size() <= max) return list;
    notes.add(
        language.pick(
            "范围太大：" + zh + "只保留了前 " + max + " 个，其余请缩小范围后再起草。",
            "Too large: only the first " + max + " " + en + " were kept; draft the rest separately."));
    return list.subList(0, max);
  }

  private I18nText name(JsonNode node) {
    String zh = node.path("zh_CN").asText("").trim();
    String en = node.path("en_US").asText("").trim();
    if (zh.isEmpty() && en.isEmpty()) return null;
    return new I18nText().zhCN(zh.isEmpty() ? null : truncate(zh, 100)).enUS(en.isEmpty() ? null : truncate(en, 100));
  }

  private static String display(I18nText name) {
    return name.getZhCN() != null ? name.getZhCN() : name.getEnUS();
  }

  private static boolean validEnum(String value, Object[] values) {
    for (var v : values) if (v.toString().equals(value)) return true;
    return false;
  }

  private String references(UUID userId) {
    var refs = json.createObjectNode();
    refs.set(
        "characters",
        json.valueToTree(existing.referenceCharacters(userId, REFERENCE_CHARACTERS)));
    refs.set("fruits", json.valueToTree(existing.referenceFruits(userId, REFERENCE_FRUITS)));
    return refs.toString();
  }

  /** What one detail call is asked to fill in, with the relations as context. */
  private String detailRequest(List<Entry> batch, Map<String, Entry> byKey) {
    ArrayNode request = json.createArrayNode();
    for (var entry : batch) {
      var node = request.addObject();
      node.put("key", entry.key);
      node.put("type", entry.type);
      node.set("name", nameNode(entry.name));
      if (entry.type.equals(FRUIT) && entry.rosterData.has("fruit_type"))
        node.set("fruit_type", entry.rosterData.get("fruit_type"));
      if (!entry.fruits.isEmpty()) {
        var eats = node.putArray("eats");
        entry.fruits.forEach(k -> eats.add(display(byKey.get(k).name)));
      }
      if (entry.group != null) {
        node.put("group", display(byKey.get(entry.group).name));
        node.put("position", entry.position);
      }
    }
    return request.toString();
  }

  private ObjectNode nameNode(I18nText name) {
    var node = json.createObjectNode();
    if (name.getZhCN() != null) node.put("zh_CN", name.getZhCN());
    if (name.getEnUS() != null) node.put("en_US", name.getEnUS());
    return node;
  }

  /** Turns a roster entry plus its drafted details into a reviewable, schema-checked item. */
  private ImportItem toItem(Entry entry, JsonNode detail, AssistantLanguage language) {
    var data = json.createObjectNode();
    data.set("name", nameNode(entry.name));
    var uncertain = new ArrayList<String>();
    var problems = new ArrayList<String>();
    if (entry.existingId == null) {
      if (entry.type.equals(GROUP)) {
        if (entry.rosterData.has("group_category")) data.setAll(entry.rosterData);
      } else {
        if (detail == null) {
          problems.add(language.pick("AI 没有给出这一项的属性，请手动填写。", "No attributes were drafted; fill them in."));
        } else {
          sanitize(entry.type, detail.path("data"), data, uncertain);
          for (var field : detail.path("uncertain"))
            if (field.isTextual() && data.has(field.asText()) && !uncertain.contains(field.asText()))
              uncertain.add(field.asText());
        }
        // The roster decided the fruit type; the detail call only received it as context.
        if (entry.type.equals(FRUIT) && entry.rosterData.has("fruit_type"))
          data.set("fruit_type", entry.rosterData.get("fruit_type"));
        if (entry.type.equals(CHARACTER)) {
          if (!data.has("hakis")) data.putArray("hakis");
          fitFruitCapacity(entry, data, uncertain, problems, language);
        }
      }
      missingRequired(entry.type, data, problems, language);
    }
    return new ImportItem(
        entry.key,
        entry.type,
        data,
        entry.existingId,
        List.copyOf(entry.fruits),
        entry.group,
        entry.position,
        List.copyOf(uncertain),
        List.copyOf(problems));
  }

  /**
   * Copies the fields the create schema knows. A number outside its range is clamped and marked
   * uncertain; an unknown enum value or malformed field is dropped (marked uncertain when optional,
   * reported as missing when required).
   */
  void sanitize(String type, JsonNode drafted, ObjectNode data, List<String> uncertain) {
    var properties = ActionSchemas.fields(type, true).path("properties");
    drafted
        .fields()
        .forEachRemaining(
            field -> {
              String key = field.getKey();
              JsonNode schema = properties.get(key);
              if (key.equals("name") || schema == null || field.getValue().isNull()) return;
              JsonNode value = repair(field.getValue(), schema);
              if (value == null) {
                if (!uncertain.contains(key)) uncertain.add(key);
                return;
              }
              if (!value.equals(field.getValue()) && !uncertain.contains(key)) uncertain.add(key);
              data.set(key, value);
            });
  }

  /** A schema-valid version of {@code value}, or null when there is none. */
  private JsonNode repair(JsonNode value, JsonNode schema) {
    String type = schema.path("type").asText();
    switch (type) {
      case "integer", "number" -> {
        double number;
        if (value.isNumber()) number = value.asDouble();
        else if (value.isTextual()) {
          try {
            number = Double.parseDouble(value.asText().replace(",", "").trim());
          } catch (NumberFormatException e) {
            return null;
          }
        } else return null;
        if (!Double.isFinite(number)) return null;
        number = Math.max(schema.path("minimum").asDouble(), Math.min(schema.path("maximum").asDouble(), number));
        if (type.equals("integer")) {
          long rounded = Math.round(number);
          return value.isIntegralNumber() && value.asLong() == rounded
              ? value
              : json.getNodeFactory().numberNode(rounded);
        }
        return value.isNumber() && value.asDouble() == number
            ? value
            : json.getNodeFactory().numberNode(Math.round(number * 100) / 100.0);
      }
      case "string" -> {
        if (!value.isTextual() && !value.isNumber()) return null;
        String text = value.asText().trim();
        if (text.isEmpty()) return null;
        if (schema.has("enum")) {
          for (var option : schema.get("enum"))
            if (option.asText().equalsIgnoreCase(text)) return option;
          return null;
        }
        int max = schema.path("maxLength").asInt(Integer.MAX_VALUE);
        return text.equals(value.asText()) && text.length() <= max
            ? value
            : json.getNodeFactory().textNode(truncate(text, max));
      }
      case "array" -> {
        if (!value.isArray()) return null;
        var result = json.createArrayNode();
        var seen = new HashSet<JsonNode>();
        for (var element : value) {
          JsonNode repaired = repair(element, schema.get("items"));
          if (repaired != null && seen.add(repaired)) result.add(repaired);
          if (result.size() == schema.path("maxItems").asInt(30)) break;
        }
        return result;
      }
      default -> {
        return null;
      }
    }
  }

  private static String truncate(String text, int max) {
    return text.length() <= max ? text : text.substring(0, max);
  }

  /** A character that eats more fruits than its capacity gets the capacity raised (max 2). */
  private void fitFruitCapacity(
      Entry entry,
      ObjectNode data,
      List<String> uncertain,
      List<String> problems,
      AssistantLanguage language) {
    int fruits = entry.fruits.size();
    if (fruits > 2) {
      problems.add(
          language.pick(
              "一个角色最多吃 2 个果实，请删掉多余的果实。",
              "A character can hold at most 2 fruits; remove the extra ones."));
    } else if (fruits > data.path("maximum_fruit_count").asInt(0)) {
      data.put("maximum_fruit_count", Math.max(fruits, 1));
      if (!uncertain.contains("maximum_fruit_count")) uncertain.add("maximum_fruit_count");
    }
  }

  private static void missingRequired(
      String type, ObjectNode data, List<String> problems, AssistantLanguage language) {
    var missing = new ArrayList<String>();
    for (var required : ActionSchemas.fields(type, true).path("required"))
      if (!data.has(required.asText())) missing.add(required.asText());
    if (!missing.isEmpty())
      problems.add(
          language.pick("缺少必填项：", "Missing required fields: ") + String.join(", ", missing));
  }
}
