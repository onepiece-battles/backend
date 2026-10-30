package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
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
import java.util.concurrent.ConcurrentHashMap;
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
  /**
   * DeepSeek's thinking model ("深度思考") does the import's knowledge work: listing a crew,
   * checking fruits, drafting facts and reading revision requests, nicknames, context and real
   * names included. Its thinking counts toward its output limit.
   */
  static final String REASONING_MODEL = "deepseek-reasoner";
  static final int REASONING_MAX_TOKENS = 32000;
  static final int MAX_HISTORY = 12;
  static final int MAX_TURN_LENGTH = 2000;

  private final AiSettingService settings;
  private final ChatCompletionClient client;
  private final ExistingResources existing;
  private final CharacterFacts facts;
  private final Executor detailExecutor;
  private final ObjectMapper json = new ObjectMapper();

  @Autowired
  public ImportDrafter(
      AiSettingService settings,
      ChatCompletionClient client,
      ExistingResources existing,
      CharacterFacts facts,
      AssistantExecutors executors) {
    this(settings, client, existing, facts, executors.tools());
  }

  ImportDrafter(
      AiSettingService settings,
      ChatCompletionClient client,
      ExistingResources existing,
      CharacterFacts facts,
      Executor detailExecutor) {
    this.settings = settings;
    this.client = client;
    this.existing = existing;
    this.facts = facts;
    this.detailExecutor = detailExecutor;
  }

  private record Settings(String baseUrl, String apiKey, String model) {}

  /** A roster entry before details are filled in. */
  private static final class Entry {
    final String key;
    final String type;
    I18nText name;
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

  private Settings connect(AssistantLanguage language) {
    if (!settings.isConfigured()) {
      throw new AssistantException(
          AssistantException.NOT_CONFIGURED,
          language.pick("还没有配置 DeepSeek Key。", "No DeepSeek key is configured yet."));
    }
    return new Settings(
        settings.baseUrl(), settings.apiKey(), AssistantAgent.resolveModel(settings.model()));
  }

  public ImportDraft draft(
      String scope, UUID userId, AssistantLanguage language, Consumer<String> progress) {
    var s = connect(language);
    var notes = new ArrayList<String>();

    progress.accept(language.pick("正在梳理名单…", "Listing who and what is in scope…"));
    JsonNode roster = think(s, ImportPrompts.ROSTER, scope);
    List<Entry> entries = readRoster(roster, notes, language);
    if (!entries.isEmpty()) {
      progress.accept(language.pick("正在核对每个角色的果实…", "Checking each character's devil fruit…"));
      entries = checkFruits(s, entries, notes, language);
    }
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
    entries = reuseHeldFruits(entries, notes, language);
    noteUnheldFruits(entries, notes, language);

    var byKey = new HashMap<String, Entry>();
    entries.forEach(e -> byKey.put(e.key, e));
    var details = details(s, entries, byKey, userId, language, progress);

    progress.accept(language.pick("正在校验草稿…", "Checking the draft…"));
    var items = new ArrayList<ImportItem>();
    for (var entry : entries) items.add(toItem(entry, details.get(entry.key), language));
    return new ImportDraft(items, notes);
  }

  /**
   * Drafts the create fields of the new characters and fruits among {@code entries}, in parallel
   * batches, by entry key. {@code byKey} names their relations for context; characters also get
   * the facts {@link CharacterFacts} finds.
   */
  private Map<String, JsonNode> details(
      Settings s,
      List<Entry> entries,
      Map<String, Entry> byKey,
      UUID userId,
      AssistantLanguage language,
      Consumer<String> progress) {
    var toDetail =
        entries.stream().filter(e -> e.existingId == null && !e.type.equals(GROUP)).toList();
    Map<String, JsonNode> details = new HashMap<>();
    if (toDetail.isEmpty()) return details;
    var found = new ConcurrentHashMap<String, Map<String, String>>();
    if (toDetail.stream().anyMatch(e -> e.type.equals(CHARACTER))) {
      progress.accept(
          language.pick(
              "正在核对角色资料（百度百科 / Bangumi）…", "Checking character facts (Baidu Baike / Bangumi)…"));
      toDetail.stream()
          .filter(e -> e.type.equals(CHARACTER))
          .map(
              e ->
                  CompletableFuture.runAsync(
                      () -> {
                        var known = facts.find(e.name);
                        if (!known.isEmpty()) found.put(e.key, known);
                      },
                      detailExecutor))
          .toList()
          .forEach(CompletableFuture::join);
    }
    String references = references(userId);
    var batches = new ArrayList<List<Entry>>();
    for (int i = 0; i < toDetail.size(); i += BATCH_SIZE)
      batches.add(toDetail.subList(i, Math.min(i + BATCH_SIZE, toDetail.size())));
    progress.accept(
        language.pick(
            "正在起草 " + toDetail.size() + " 项属性（" + batches.size() + " 批）…",
            "Drafting " + toDetail.size() + " items in " + batches.size() + " batches…"));
    var futures =
        batches.stream()
            .map(
                batch ->
                    CompletableFuture.supplyAsync(
                        () ->
                            think(
                                s,
                                ImportPrompts.detail(references),
                                detailRequest(batch, byKey, found)),
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
    // A source's fact wins over the model's memory; a fact the model is unsure of and no source
    // gives stays blank.
    for (var entry : toDetail) {
      if (!entry.type.equals(CHARACTER)) continue;
      JsonNode detail = details.get(entry.key);
      if (detail instanceof ObjectNode object) {
        var data =
            object.path("data") instanceof ObjectNode fields ? fields : json.createObjectNode();
        object.set("data", data);
        var verified = CharacterFactValues.read(found.getOrDefault(entry.key, Map.of()));
        var unsure = new LinkedHashSet<String>();
        detail.path("uncertain").forEach(key -> unsure.add(key.asText()));
        for (String key : ImportRevisionScope.PROFILE)
          if (verified.containsKey(key)) {
            data.set(key, verified.get(key));
            unsure.remove(key);
          } else if (unsure.contains(key)) data.remove(key);
        var uncertain = object.putArray("uncertain");
        unsure.forEach(uncertain::add);
      }
    }
    return details;
  }

  /**
   * Applies a reviewer's request to the current draft with one call to DeepSeek's thinking model
   * ({@link ImportPrompts#REVISE}): it works out what the request means in the conversation and
   * finds the values itself, real names, facts and stats on the app's scale included. The server
   * keeps the answer to what the request refers to ({@link ImportRevisionScope}), lets Baidu Baike
   * and Bangumi correct facts (what neither the model nor a source knows stays blank), and drafts
   * added items as in {@link #draft}. Everything else keeps the reviewer's own edits, and the
   * reply says exactly what changed.
   */
  public ImportRevision revise(
      List<ImportItem> current,
      List<Turn> history,
      String instruction,
      UUID userId,
      AssistantLanguage language,
      Consumer<String> progress) {
    var s = connect(language);
    progress.accept(
        language.pick("DeepSeek 正在深度思考你的修改…", "DeepSeek is thinking the changes through…"));
    var scope = new ImportRevisionScope(current, history, instruction);
    var conversation = new ArrayList<ObjectNode>();
    var turns = history == null ? List.<Turn>of() : history;
    for (var turn : turns.subList(Math.max(0, turns.size() - MAX_HISTORY), turns.size()))
      if (turn != null
          && turn.content() != null
          && Set.of("user", "assistant").contains(turn.role()))
        conversation.add(message(turn.role(), truncate(turn.content(), MAX_TURN_LENGTH)));
    conversation.add(
        message(
            "user",
            "Current draft:\n"
                + roster(current)
                + "\n\nReference characters and fruits of the app: "
                + references(userId)
                + "\n\nRequest: "
                + instruction));
    var messages = new ArrayList<ObjectNode>();
    messages.add(message("system", ImportPrompts.REVISE));
    messages.addAll(alternating(conversation));
    JsonNode answer = think(s, messages);

    var items = new LinkedHashMap<String, ImportItem>();
    for (var item : current) if (item != null && item.key() != null) items.put(item.key(), item);
    // Keys of removed items are not handed out again: the page keeps per-key review state.
    var taken = Set.copyOf(items.keySet());
    var notes = new ArrayList<String>();
    for (var note : answer.path("notes")) if (note.isTextual()) notes.add(note.asText());
    for (var entry : answer.path("remove")) {
      var item = items.get(entry.isObject() ? entry.path("key").asText() : entry.asText());
      if (item != null && scope.reaches(item, entry.path("refers_to"))) items.remove(item.key());
    }
    // A removed character's newly drafted fruit is a dependent item, unless another holder needs
    // it.
    var removedFruits = new HashSet<String>();
    for (var item : current)
      if (!items.containsKey(item.key()) && item.type().equals(CHARACTER) && item.fruits() != null)
        removedFruits.addAll(item.fruits());
    for (var item : items.values())
      if (item.fruits() != null) removedFruits.removeAll(item.fruits());
    items
        .values()
        .removeIf(
            i ->
                i.type().equals(FRUIT)
                    && i.existingId() == null
                    && removedFruits.contains(i.key()));

    // The model's keys for new items map to server keys after the current ones; a "new" item with
    // the name of a current one maps to that item.
    var keys = new HashMap<String, String>();
    var named = new HashMap<String, String>();
    for (var item : items.values()) {
      keys.put(item.key(), item.key());
      var name = name(item.data() == null ? MissingNode.getInstance() : item.data().path("name"));
      if (name != null)
        named.put(item.type() + ":" + ExistingResources.normalize(display(name)), item.key());
    }
    var added = new ArrayList<Entry>();
    var relations = new HashMap<Entry, JsonNode>();
    JsonNode add = answer.path("add");
    Set<String> allowedAdditions = allowedAdditions(add, answer.path("changes"), items, scope);
    for (var section : List.of(GROUP, FRUIT, CHARACTER)) {
      for (var node : add.path(section + "s")) {
        if (!allowedAdditions.contains(node.path("key").asText())) continue;
        var name = name(node.path("name"));
        if (name == null) continue;
        String same = named.get(section + ":" + ExistingResources.normalize(display(name)));
        if (same != null) {
          keys.put(node.path("key").asText(), same);
          continue;
        }
        if (count(items.values(), added, section) >= limit(section)) {
          notes.add(
              language.pick(
                  "已达到单次导入的上限，没有加上「" + display(name) + "」。",
                  "The draft is full; " + display(name) + " was not added."));
          continue;
        }
        var entry = new Entry(nextKey(section, taken, added), section, name);
        if (section.equals(GROUP)) {
          String category = node.path("group_category").asText();
          if (validEnum(category, GroupCategory.values()) && !category.equals("group"))
            entry.rosterData.put("group_category", category);
        } else if (section.equals(FRUIT)) {
          String fruitType = node.path("fruit_type").asText();
          if (validEnum(fruitType, FruitType.values()))
            entry.rosterData.put("fruit_type", fruitType);
        } else relations.put(entry, node);
        keys.put(node.path("key").asText(), entry.key);
        named.put(section + ":" + ExistingResources.normalize(display(name)), entry.key);
        added.add(entry);
      }
    }
    var types = new HashMap<String, String>();
    items.values().forEach(i -> types.put(i.key(), i.type()));
    added.forEach(e -> types.put(e.key, e.type));
    for (var relation : relations.entrySet()) {
      var entry = relation.getKey();
      var node = relation.getValue();
      entry.fruits.addAll(
          linked(node.path("fruits"), FRUIT, entry.name, keys, types, notes, language));
      var group = linked(node.path("group"), GROUP, entry.name, keys, types, notes, language);
      if (!group.isEmpty()) {
        entry.group = group.get(0);
        String position = node.path("position").asText();
        entry.position = validEnum(position, GroupPosition.values()) ? position : "member";
      }
    }

    var revision =
        new Revision(scope, instruction, items, keys, types, notes, new ArrayList<>(),
            new LinkedHashMap<>(), new ArrayList<>(), new HashSet<>(), language);
    for (var change : answer.path("changes")) {
      var item = items.get(change.path("key").asText());
      if (item != null && scope.reaches(item, change.path("refers_to")))
        items.put(item.key(), apply(item, change, revision));
    }

    // A new fruit that a change took from its holder, and nobody else eats, goes too: every fruit
    // in the draft has a holder.
    var holders = new HashSet<String>();
    for (var item : items.values()) if (item.fruits() != null) holders.addAll(item.fruits());
    added.forEach(e -> holders.addAll(e.fruits));
    for (var before : current) {
      if (!revision.refruited().contains(before.key()) || before.fruits() == null) continue;
      for (var key : before.fruits()) {
        var fruit = items.get(key);
        if (fruit == null || fruit.existingId() != null || holders.contains(key)) continue;
        items.remove(key);
        var name = name(fruit.data() == null ? MissingNode.getInstance() : fruit.data().path("name"));
        String display = name == null ? key : language.displayName(name);
        notes.add(
            language.pick(
                "「" + display + "」不再有人吃，已从草稿去掉。",
                display + " is no longer eaten by anyone here, so it left the draft."));
      }
    }

    if (!added.isEmpty()) {
      progress.accept(language.pick("正在核对已有数据…", "Checking what already exists…"));
      for (var entry : added)
        existing.find(entry.type, entry.name, userId).ifPresent(id -> entry.existingId = id.toString());
      var reused = reuseHeldFruits(added, notes, language);
      var byKey = new HashMap<String, Entry>();
      for (var item : items.values()) {
        var entry = entry(item);
        if (entry != null) byKey.put(entry.key, entry);
      }
      reused.forEach(e -> byKey.put(e.key, e));
      var details = details(s, reused, byKey, userId, language, progress);
      for (var entry : reused) items.put(entry.key, toItem(entry, details.get(entry.key), language));
    }

    progress.accept(language.pick("正在校验草稿…", "Checking the draft…"));
    var result = new ArrayList<ImportItem>();
    for (var item : items.values()) result.add(checked(item, items.keySet(), language));
    var entries = new ArrayList<Entry>();
    for (var item : result) {
      var entry = entry(item);
      if (entry != null) entries.add(entry);
    }
    noteUnheldFruits(entries, notes, language);
    notes.addAll(revision.remarks());
    // Report actual data changes, never the model's unverified claim that it changed something.
    var previous = new HashMap<String, ImportItem>();
    current.forEach(i -> previous.put(i.key(), i));
    var changed = new LinkedHashSet<String>();
    for (var item : result)
      if (!sameContent(previous.get(item.key()), item)) changed.add(item.key());
    // The page looks the portrait up again; a rename alone never asks it to.
    var refreshAvatars = revision.portraits().stream().filter(items::containsKey).toList();
    changed.addAll(refreshAvatars);
    String asked = text(answer.path("question"));
    if (asked == null) asked = text(answer.path("answer"));
    String reply =
        ImportRevisionSummary.reply(
            current, result, revision.unverified(), refreshAvatars, language);
    if (reply == null)
      reply =
          asked != null
              ? asked
              : !revision.remarks().isEmpty()
                  ? String.join(" ", revision.remarks())
                  : language.pick(
                      "草稿未变更。请说明要改哪一项和改什么。",
                      "The draft is unchanged. Say which item and what to change.");
    else {
      if (!revision.remarks().isEmpty()) reply += " " + String.join(" ", revision.remarks());
      if (asked != null) reply += " " + asked;
    }
    return new ImportRevision(result, notes, reply, List.copyOf(changed), refreshAvatars);
  }

  /**
   * What applying the changes of one revision needs, and what it collects: remarks for the
   * reply, facts that stay blank, portraits to look up again and characters whose fruits changed.
   */
  private record Revision(
      ImportRevisionScope scope,
      String instruction,
      Map<String, ImportItem> items,
      Map<String, String> keys,
      Map<String, String> types,
      List<String> notes,
      List<String> remarks,
      Map<String, List<String>> unverified,
      List<String> portraits,
      Set<String> refruited,
      AssistantLanguage language) {}

  /**
   * One change of the model's answer, as far as the scope allows it: a new name ({@link #rename}),
   * create fields (a source's fact wins over the model's unless the reviewer gave the value;
   * what the model is unsure of and no source knows stays blank), relations and a portrait.
   */
  private ImportItem apply(ImportItem item, JsonNode change, Revision revision) {
    var scope = revision.scope();
    boolean fresh = item.existingId() == null;
    var data = item.data() == null ? json.createObjectNode() : item.data().deepCopy();
    var uncertain = new ArrayList<>(item.uncertain() == null ? List.<String>of() : item.uncertain());
    JsonNode set = change.path("set");
    var unsure = new LinkedHashSet<String>();
    change.path("unsure").forEach(f -> unsure.add(f.asText()));
    if (fresh
        && item.type().equals(CHARACTER)
        && (set.has("name") || unsure.contains("name"))
        && scope.allows(item, "name")) {
      var renamed = rename(item, set.path("name"), change.path("impersonates"), revision);
      if (renamed != null) data.set("name", renamed);
    } else if (fresh && set.path("name").isObject() && scope.allows(item, "name")) {
      var renamed = renamedAsGiven(data.path("name"), set.get("name"), scope);
      if (renamed != null) data.set("name", renamed);
    }
    if (fresh) {
      var patch = json.createObjectNode();
      var blank = new LinkedHashSet<String>();
      set.fields()
          .forEachRemaining(
              field -> {
                String key = field.getKey();
                if (key.equals("name")
                    || key.equals("avatar")
                    || ImportRevisionScope.RELATIONS.contains(key)
                    || !scope.allows(item, key)) return;
                if (field.getValue().isNull() || unsure.contains(key)) blank.add(key);
                else patch.set(key, field.getValue());
              });
      var who = name(data.path("name"));
      if (item.type().equals(CHARACTER)
          && who != null
          && ImportRevisionScope.PROFILE.stream().anyMatch(f -> patch.has(f) || unsure.contains(f))) {
        var verified = CharacterFactValues.read(facts.find(who));
        for (String key : ImportRevisionScope.PROFILE) {
          if (!verified.containsKey(key) || !scope.allows(item, key)) continue;
          if (patch.has(key) && !CharacterFactValues.given(revision.instruction(), key, patch.get(key))
              || blank.contains(key) && unsure.contains(key)) {
            patch.set(key, verified.get(key));
            blank.remove(key);
          }
        }
      }
      for (String key : blank) {
        if (required(item.type(), key)) continue;
        data.remove(key);
        uncertain.remove(key);
        if (unsure.contains(key)) {
          uncertain.add(key);
          revision.unverified().computeIfAbsent(item.key(), k -> new ArrayList<>()).add(key);
        }
      }
      patch.fieldNames().forEachRemaining(uncertain::remove);
      sanitize(item.type(), patch, data, uncertain);
    }
    var fruits = item.fruits();
    var group = item.group();
    var position = item.position();
    if (item.type().equals(CHARACTER)) {
      var who = name(data.path("name"));
      var language = revision.language();
      JsonNode newFruits = change.has("fruits") ? change.get("fruits") : set.get("fruits");
      if (newFruits != null && scope.allows(item, "fruits")) {
        fruits =
            linked(newFruits, FRUIT, who, revision.keys(), revision.types(), revision.notes(), language);
        revision.refruited().add(item.key());
      }
      JsonNode newGroup = change.has("group") ? change.get("group") : set.get("group");
      if (newGroup != null && scope.allows(item, "group")) {
        var linked =
            linked(newGroup, GROUP, who, revision.keys(), revision.types(), revision.notes(), language);
        group = linked.isEmpty() ? null : linked.get(0);
        if (group == null) position = null;
        else if (position == null) position = "member";
      }
      String requested =
          (change.has("position") ? change.path("position") : set.path("position")).asText();
      if (scope.allows(item, "position")
          && group != null
          && validEnum(requested, GroupPosition.values())) position = requested;
    }
    if ((change.path("avatar").asBoolean(false) || set.path("avatar").asBoolean(false))
        && scope.allows(item, "avatar")) revision.portraits().add(item.key());
    return new ImportItem(
        item.key(), item.type(), data, item.existingId(), fruits, group, position, uncertain,
        List.of());
  }

  /** A name the request gives a fruit or group, in the languages it may change. */
  private ObjectNode renamedAsGiven(JsonNode old, JsonNode given, ImportRevisionScope scope) {
    var renamed = old instanceof ObjectNode o ? o.deepCopy() : json.createObjectNode();
    for (String locale : List.of("zh_CN", "en_US")) {
      String value = given.path(locale).asText("").trim();
      if (!value.isEmpty() && scope.allowsName(locale)) renamed.put(locale, truncate(value, 100));
    }
    return renamed;
  }

  /**
   * The name a change gives a new character: as the request gives it; else DeepSeek's answer,
   * spelled as Baidu Baike or Bangumi spell it when they know it; else, when DeepSeek has none,
   * the name a source knows the drafted one as an alias of (假路飞 → 丹马罗·布拉克). The name of
   * the one he impersonates, or of another item, is not taken.
   */
  private ObjectNode rename(
      ImportItem item, JsonNode proposed, JsonNode impersonates, Revision revision) {
    var language = revision.language();
    var old = item.data() != null && item.data().path("name") instanceof ObjectNode n ? n : null;
    var drafted = old == null ? null : name(old);
    String who = drafted == null ? item.key() : language.displayName(drafted);
    var impostor = impersonates.isObject() ? name(impersonates) : null;
    var answer = proposed.isObject() ? name(proposed) : null;
    Map<String, String> known = Map.of();
    boolean given = answer != null && literal(revision.instruction(), answer);
    if (answer != null && !given) known = facts.find(answer);
    else if (answer == null && drafted != null) {
      known = facts.find(drafted);
      if (impostor != null && ExistingResources.sameName(canonical(known), impostor)) known = Map.of();
      var alias = canonical(known);
      if (alias.getZhCN() != null || alias.getEnUS() != null) answer = alias;
    }
    if (answer == null) {
      revision
          .remarks()
          .add(
              language.pick(
                  "「" + who + "」：DeepSeek 和百度百科 / Bangumi 都没有给出可靠的真实姓名，名字保持不变。",
                  who + ": neither DeepSeek nor Baidu Baike / Bangumi knows the real name, so it"
                      + " stays."));
      return null;
    }
    var candidate = answer;
    boolean someoneElse =
        impostor != null && ExistingResources.sameName(impostor, candidate)
            || revision.items().values().stream()
                .filter(other -> !other.key().equals(item.key()) && other.data() != null)
                .anyMatch(
                    other -> ExistingResources.sameName(name(other.data().path("name")), candidate));
    if (someoneElse) {
      revision
          .remarks()
          .add(
              language.pick(
                  "「" + who + "」：「" + language.displayName(candidate) + "」是另一个角色的名字，没有采用。",
                  who + ": " + language.displayName(candidate) + " is someone else's name, so it"
                      + " was not used."));
      return null;
    }
    var renamed = old == null ? json.createObjectNode() : old.deepCopy();
    var scope = revision.scope();
    for (String locale : List.of("zh_CN", "en_US")) {
      if (!scope.allowsName(locale)) continue;
      String value = locale.equals("zh_CN") ? candidate.getZhCN() : candidate.getEnUS();
      if (value == null || value.isBlank()) continue;
      String spelled = given ? null : known.get("canonical_" + locale);
      renamed.put(locale, truncate((spelled == null || spelled.isBlank() ? value : spelled).trim(), 100));
    }
    if (!given && known.isEmpty() && !renamed.equals(old))
      revision
          .notes()
          .add(
              language.pick(
                  "「" + who + "」的真名来自 DeepSeek，百度百科 / Bangumi 里没有收录，可以再确认一下。",
                  who + "'s real name comes from DeepSeek; Baidu Baike / Bangumi do not list it."));
    return renamed;
  }

  /** Whether the request itself gives this name. */
  private static boolean literal(String instruction, I18nText name) {
    if (instruction == null) return false;
    return name.getZhCN() != null && instruction.contains(name.getZhCN())
        || name.getEnUS() != null
            && instruction.toLowerCase(Locale.ROOT).contains(name.getEnUS().toLowerCase(Locale.ROOT));
  }

  /**
   * The additions the request asks for ({@link ImportRevisionScope#allowsAddition}) and what they
   * need: an added character's fruit, an added fruit's holder, and the new fruit or group a
   * change to an item the request refers to points at. Never other new items.
   */
  private static Set<String> allowedAdditions(
      JsonNode add, JsonNode changes, Map<String, ImportItem> items, ImportRevisionScope scope) {
    Set<String> allowed = new HashSet<>();
    Map<String, JsonNode> characters = new LinkedHashMap<>();
    Set<String> fruits = new HashSet<>();
    for (String type : List.of(GROUP, FRUIT, CHARACTER))
      for (JsonNode node : add.path(type + "s")) {
        String key = node.path("key").asText();
        if (type.equals(CHARACTER)) characters.put(key, node);
        if (type.equals(FRUIT)) fruits.add(key);
        String zh = node.path("name").path("zh_CN").asText("").trim();
        String en = node.path("name").path("en_US").asText("").trim();
        if (scope.allowsAddition(
            new I18nText().zhCN(zh.isEmpty() ? null : zh).enUS(en.isEmpty() ? null : en)))
          allowed.add(key);
      }
    for (var change : changes) {
      var item = items.get(change.path("key").asText());
      if (item == null || !scope.reaches(item, change.path("refers_to"))) continue;
      if (scope.allows(item, "fruits")) {
        change.path("fruits").forEach(f -> allowed.add(f.asText()));
        change.path("set").path("fruits").forEach(f -> allowed.add(f.asText()));
      }
      if (scope.allows(item, "group")) {
        if (change.path("group").isTextual()) allowed.add(change.path("group").asText());
        if (change.path("set").path("group").isTextual())
          allowed.add(change.path("set").path("group").asText());
      }
    }
    for (var character : characters.entrySet()) {
      if (!allowed.contains(character.getKey()))
        for (JsonNode fruit : character.getValue().path("fruits"))
          if (allowed.contains(fruit.asText()) && fruits.contains(fruit.asText())) {
            allowed.add(character.getKey());
            break;
          }
      if (allowed.contains(character.getKey()))
        for (JsonNode fruit : character.getValue().path("fruits"))
          if (fruits.contains(fruit.asText())) allowed.add(fruit.asText());
    }
    return allowed;
  }

  /**
   * One JSON answer from DeepSeek's thinking model ("深度思考"), or from the chat model when the
   * thinking model is not available (another provider, an outage).
   */
  private JsonNode think(Settings s, List<ObjectNode> messages) {
    if (!s.model().equals(REASONING_MODEL))
      try {
        return ask(
            new Settings(s.baseUrl(), s.apiKey(), REASONING_MODEL), messages, REASONING_MAX_TOKENS);
      } catch (AssistantException e) {
        logger.warn("import call with {} failed; using {}", REASONING_MODEL, s.model(), e);
      }
    return ask(s, messages, MAX_TOKENS);
  }

  /**
   * The conversation as the thinking model takes it: starting with the reviewer and alternating,
   * so consecutive turns of one side (a request that failed, say) are joined.
   */
  private static List<ObjectNode> alternating(List<ObjectNode> turns) {
    var result = new ArrayList<ObjectNode>();
    for (var turn : turns) {
      String role = turn.path("role").asText();
      if (result.isEmpty() && !role.equals("user")) continue;
      var last = result.isEmpty() ? null : result.get(result.size() - 1);
      if (last != null && last.path("role").asText().equals(role))
        last.put("content", last.path("content").asText() + "\n\n" + turn.path("content").asText());
      else result.add(turn.deepCopy());
    }
    return result;
  }

  private JsonNode think(Settings s, String system, String user) {
    return think(s, List.of(message("system", system), message("user", user)));
  }

  /** The names a source gives as the character's own. */
  private static I18nText canonical(Map<String, String> found) {
    return new I18nText().zhCN(found.get("canonical_zh_CN")).enUS(found.get("canonical_en_US"));
  }

  private static String text(JsonNode node) {
    return node.isTextual() && !node.asText().isBlank() ? node.asText().trim() : null;
  }

  /**
   * Asks once more, per character, which devil fruit they ate: the roster call lists many entities
   * at once and tends to miss a fruit or give it to the wrong person. Missing fruits are added and
   * wrong holders corrected; a drafted fruit nobody ate is dropped with a note. If the check fails
   * the roster stands.
   */
  private List<Entry> checkFruits(
      Settings s, List<Entry> entries, List<String> notes, AssistantLanguage language) {
    var characters = entries.stream().filter(e -> e.type.equals(CHARACTER)).toList();
    if (characters.isEmpty()) return entries;
    var byKey = new HashMap<String, Entry>();
    entries.forEach(e -> byKey.put(e.key, e));
    var request = json.createObjectNode();
    var list = request.putArray("characters");
    for (var character : characters) {
      var node = list.addObject().put("key", character.key);
      node.set("name", nameNode(character.name));
      var listed = node.putArray("listed_fruits");
      character.fruits.forEach(k -> listed.add(nameNode(byKey.get(k).name)));
    }
    JsonNode answer;
    try {
      answer = think(s, ImportPrompts.FRUITS, request.toString());
    } catch (RuntimeException e) {
      logger.warn("import fruit check failed; keeping the roster's fruits", e);
      return entries;
    }

    var result = new ArrayList<>(entries);
    var fruitsByName = new HashMap<String, Entry>();
    for (var entry : entries) if (entry.type.equals(FRUIT)) indexFruit(fruitsByName, entry);
    for (var item : answer.path("characters")) {
      var character = byKey.get(item.path("key").asText());
      if (character == null || !character.type.equals(CHARACTER) || !item.has("fruit")) continue;
      character.fruits.clear();
      var name = item.get("fruit").isObject() ? name(item.get("fruit")) : null;
      if (name == null) continue;
      var fruit = findFruit(fruitsByName, name);
      if (fruit == null) {
        if (result.stream().filter(e -> e.type.equals(FRUIT)).count() >= MAX_FRUITS) continue;
        var keys = new HashSet<String>();
        result.forEach(e -> keys.add(e.key));
        fruit = new Entry(nextKey(FRUIT, keys, List.of()), FRUIT, name);
        String fruitType = item.get("fruit").path("fruit_type").asText();
        if (validEnum(fruitType, FruitType.values())) fruit.rosterData.put("fruit_type", fruitType);
        int firstCharacter = result.indexOf(characters.get(0));
        result.add(firstCharacter < 0 ? result.size() : firstCharacter, fruit);
        indexFruit(fruitsByName, fruit);
      }
      character.fruits.add(fruit.key);
    }
    var held = new HashSet<String>();
    characters.forEach(c -> held.addAll(c.fruits));
    result.removeIf(
        e -> {
          if (!e.type.equals(FRUIT) || held.contains(e.key)) return false;
          String fruit = language.displayName(e.name);
          notes.add(
              language.pick(
                  "「" + fruit + "」：核对后没有角色吃这个果实，已从草稿去掉；需要的话告诉 AI 它的持有者。",
                  fruit
                      + ": no character here ate this fruit, so it was left out; tell the AI who"
                      + " ate it if it belongs."));
          return true;
        });
    return result;
  }

  /**
   * A drafted character who already exists in the app may already hold their fruit, perhaps
   * under a slightly different name than the draft's: reuse it instead of creating a
   * near-duplicate. A new fruit drafted for them becomes the one they hold (named as in the app);
   * a held fruit the draft did not give them is linked; a drafted duplicate is dropped.
   */
  private List<Entry> reuseHeldFruits(
      List<Entry> entries, List<String> notes, AssistantLanguage language) {
    var result = new ArrayList<>(entries);
    var byKey = new HashMap<String, Entry>();
    entries.forEach(e -> byKey.put(e.key, e));
    var merged = new HashSet<String>();
    for (var character : entries) {
      if (!character.type.equals(CHARACTER) || character.existingId == null) continue;
      List<ExistingResources.Held> held;
      try {
        held = existing.heldFruits(UUID.fromString(character.existingId));
      } catch (RuntimeException e) {
        continue;
      }
      if (held == null || held.isEmpty()) continue;
      var remaining = new ArrayList<>(held);
      var fruits = new ArrayList<String>();
      for (var key : character.fruits) {
        var fruit = byKey.get(key);
        ExistingResources.Held match = null;
        if (fruit != null) {
          for (var h : remaining)
            if (fruit.existingId != null
                ? h.id().toString().equals(fruit.existingId)
                : ExistingResources.sameFruit(h.name(), fruit.name)) match = h;
          // Someone holds one fruit: a new one drafted for them is that one under another name.
          if (match == null && fruit.existingId == null && remaining.size() == 1)
            match = remaining.get(0);
        }
        if (match == null) {
          fruits.add(key);
          continue;
        }
        remaining.remove(match);
        fruits.add(reuse(result, fruit, match, merged, notes, language).key);
      }
      for (var match : remaining) fruits.add(reuse(result, null, match, merged, notes, language).key);
      character.fruits.clear();
      fruits.stream().distinct().forEach(character.fruits::add);
    }
    var holders = new HashSet<String>();
    result.forEach(e -> holders.addAll(e.fruits));
    result.removeIf(e -> merged.contains(e.key) && !holders.contains(e.key));
    return result;
  }

  /**
   * The draft's entry for the held fruit {@code match}: one already reusing it, else {@code
   * drafted} turned into it, else a new reused entry. A drafted fruit that turns out to duplicate
   * another entry is marked {@code merged}.
   */
  private Entry reuse(
      List<Entry> result,
      Entry drafted,
      ExistingResources.Held match,
      Set<String> merged,
      List<String> notes,
      AssistantLanguage language) {
    for (var entry : result)
      if (entry.type.equals(FRUIT) && match.id().toString().equals(entry.existingId)) {
        if (drafted != null && drafted != entry) merged.add(drafted.key);
        return entry;
      }
    if (drafted != null) {
      if (match.name() != null && !ExistingResources.sameName(match.name(), drafted.name)) {
        notes.add(
            language.pick(
                "「" + display(drafted.name) + "」就是已有的「" + display(match.name()) + "」，直接复用。",
                display(drafted.name)
                    + " is the existing "
                    + display(match.name())
                    + "; it is reused."));
        drafted.name = match.name();
      }
      drafted.existingId = match.id().toString();
      return drafted;
    }
    var keys = new HashSet<String>();
    result.forEach(e -> keys.add(e.key));
    var entry = new Entry(nextKey(FRUIT, keys, List.of()), FRUIT, match.name());
    entry.existingId = match.id().toString();
    int firstCharacter = -1;
    for (int i = 0; i < result.size() && firstCharacter < 0; i++)
      if (result.get(i).type.equals(CHARACTER)) firstCharacter = i;
    result.add(firstCharacter < 0 ? result.size() : firstCharacter, entry);
    return entry;
  }

  private static void indexFruit(Map<String, Entry> byName, Entry fruit) {
    for (var text : new String[] {fruit.name.getEnUS(), fruit.name.getZhCN()})
      if (text != null && !text.isBlank()) byName.putIfAbsent(ExistingResources.normalize(text), fruit);
  }

  /** The drafted fruit with this name, by its romanized name first (translations vary). */
  private static Entry findFruit(Map<String, Entry> byName, I18nText name) {
    for (var text : new String[] {name.getEnUS(), name.getZhCN()}) {
      if (text == null || text.isBlank()) continue;
      var fruit = byName.get(ExistingResources.normalize(text));
      if (fruit != null) return fruit;
    }
    return null;
  }

  /** The draft as the revise prompt shows it: relations, and the editable fields of new items. */
  private String roster(List<ImportItem> items) {
    var roster = json.createObjectNode();
    for (var type : List.of(GROUP, FRUIT, CHARACTER)) {
      var list = roster.putArray(type + "s");
      for (var item : items) {
        if (item == null || !type.equals(item.type())) continue;
        var node = list.addObject().put("key", item.key());
        var data = item.data() == null ? json.createObjectNode() : item.data();
        node.set("name", data.path("name").deepCopy());
        node.put("existing", item.existingId() != null);
        if (type.equals(CHARACTER)) {
          node.set("fruits", json.valueToTree(item.fruits() == null ? List.of() : item.fruits()));
          if (item.group() != null)
            node.put("group", item.group()).put("position", item.position());
        }
        if (item.existingId() == null) {
          var fields = data.deepCopy();
          fields.remove("name");
          node.set("data", fields);
          if (item.uncertain() != null && !item.uncertain().isEmpty())
            node.set("uncertain", json.valueToTree(item.uncertain()));
        }
      }
    }
    return roster.toString();
  }

  /**
   * The keys of {@code type} that {@code refs} (a key or an array of keys) points to. A reference
   * to no such item is dropped with a note, as when the draft is first read.
   */
  private static List<String> linked(
      JsonNode refs,
      String type,
      I18nText character,
      Map<String, String> keys,
      Map<String, String> types,
      List<String> notes,
      AssistantLanguage language) {
    var result = new ArrayList<String>();
    Iterable<JsonNode> values = refs.isArray() ? refs : refs.isTextual() ? List.of(refs) : List.of();
    for (var ref : values) {
      String key = keys.get(ref.asText());
      if (key == null || !type.equals(types.get(key))) {
        if (character != null && !ref.asText().isBlank())
          notes.add(
              droppedLink(
                  character, ref.asText(), type.equals(FRUIT) ? "果实" : "团体", type, language));
      } else if (!result.contains(key)) result.add(key);
    }
    return result;
  }

  /** The item checked again: capacity fitted to its fruits, required fields, dangling links cut. */
  private ImportItem checked(ImportItem item, Set<String> present, AssistantLanguage language) {
    var fruits =
        (item.fruits() == null ? List.<String>of() : item.fruits())
            .stream().filter(present::contains).toList();
    String group = present.contains(item.group()) ? item.group() : null;
    if (item.existingId() != null)
      return new ImportItem(
          item.key(), item.type(), item.data(), item.existingId(), fruits, group,
          group == null ? null : item.position(), List.of(), List.of());
    var data = item.data() == null ? json.createObjectNode() : item.data().deepCopy();
    var uncertain = new ArrayList<>(item.uncertain() == null ? List.<String>of() : item.uncertain());
    var problems = new ArrayList<String>();
    if (item.type().equals(CHARACTER)) {
      fitFruitCapacity(fruits.size(), data, problems, language);
    }
    missingRequired(item.type(), data, problems, language);
    return new ImportItem(
        item.key(), item.type(), data, null, fruits, group, group == null ? null : item.position(),
        uncertain, problems);
  }

  private Entry entry(ImportItem item) {
    var name = name(item.data() == null ? MissingNode.getInstance() : item.data().path("name"));
    if (name == null) return null;
    var entry = new Entry(item.key(), item.type(), name);
    entry.existingId = item.existingId();
    if (item.fruits() != null) entry.fruits.addAll(item.fruits());
    entry.group = item.group();
    entry.position = item.position();
    return entry;
  }

  private static int limit(String type) {
    return switch (type) {
      case GROUP -> MAX_GROUPS;
      case FRUIT -> MAX_FRUITS;
      default -> MAX_CHARACTERS;
    };
  }

  private static long count(Collection<ImportItem> items, List<Entry> added, String type) {
    return items.stream().filter(i -> type.equals(i.type())).count()
        + added.stream().filter(e -> e.type.equals(type)).count();
  }

  /** g4 after g1–g3: one past the highest number the type's prefix has had in this draft. */
  private static String nextKey(String type, Set<String> current, List<Entry> added) {
    String prefix = type.substring(0, 1);
    int max = 0;
    var all = new ArrayList<>(current);
    added.forEach(e -> all.add(e.key));
    for (var key : all) {
      if (key.startsWith(prefix) && key.substring(1).matches("\\d+"))
        max = Math.max(max, Integer.parseInt(key.substring(1)));
    }
    return prefix + (max + 1);
  }

  /** One JSON-mode completion; a reply that is not a JSON object is retried once. */
  private JsonNode ask(Settings s, List<ObjectNode> messages, int maxTokens) {
    for (int attempt = 1; ; attempt++) {
      var completion =
          client.complete(
              new CompletionRequest(
                  s.baseUrl(), s.apiKey(), s.model(), messages, null, TEMPERATURE, maxTokens, true),
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

  /** A note for a roster relation whose key matches no listed fruit or group. */
  private static String droppedLink(
      I18nText character, String reference, String zh, String en, AssistantLanguage language) {
    String who = language.displayName(character);
    return language.pick(
        "「" + who + "」的" + zh + "「" + reference + "」不在名单里，已忽略，请确认。",
        who + ": the " + en + " \"" + reference + "\" is not in the roster and was left out.");
  }

  /**
   * New fruits that no character in the draft eats. Usually the model left out (or made up) the
   * holder, and an unheld fruit is easy to import by mistake.
   */
  private static void noteUnheldFruits(
      List<Entry> entries, List<String> notes, AssistantLanguage language) {
    var eaten = new HashSet<String>();
    entries.forEach(entry -> eaten.addAll(entry.fruits));
    for (var entry : entries) {
      if (!entry.type.equals(FRUIT) || entry.existingId != null || eaten.contains(entry.key))
        continue;
      String fruit = language.displayName(entry.name);
      notes.add(
          language.pick(
              "「" + fruit + "」：草稿里没有角色吃这个果实，请确认是否需要导入。",
              fruit + ": no character in this draft eats this fruit; check that it belongs."));
    }
  }

  /**
   * Reads the roster with keys reassigned by the server (g1, f1, c1, …): the model's keys only link
   * its own entries. Unknown categories/types/positions are dropped, relations to unlisted keys are
   * dropped with a note, and duplicate names keep their first entry.
   */
  private List<Entry> readRoster(JsonNode roster, List<String> notes, AssistantLanguage language) {
    for (var note : roster.path("notes")) if (note.isTextual()) notes.add(note.asText());
    var entries = new ArrayList<Entry>();
    var groupKeys = new HashMap<String, String>();
    var fruitKeys = new HashMap<String, String>();
    var seen = new HashSet<String>();

    for (var g : limit(roster.path("groups"), MAX_GROUPS, notes, language, "团体", "groups")) {
      var name = name(g.path("name"));
      if (name == null || !seen.add(GROUP + ":" + ExistingResources.normalize(display(name))))
        continue;
      var entry = new Entry("g" + (groupKeys.size() + 1), GROUP, name);
      String category = g.path("group_category").asText();
      if (validEnum(category, GroupCategory.values()) && !category.equals("group"))
        entry.rosterData.put("group_category", category);
      groupKeys.put(g.path("key").asText(), entry.key);
      entries.add(entry);
    }
    for (var f : limit(roster.path("fruits"), MAX_FRUITS, notes, language, "果实", "fruits")) {
      var name = name(f.path("name"));
      if (name == null || !seen.add(FRUIT + ":" + ExistingResources.normalize(display(name))))
        continue;
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
        if (key == null) notes.add(droppedLink(name, fruit.asText(), "果实", "fruit", language));
        else if (!entry.fruits.contains(key)) entry.fruits.add(key);
      }
      String group = c.path("group").asText("");
      entry.group = groupKeys.get(group);
      if (entry.group == null && !group.isBlank())
        notes.add(droppedLink(name, group, "团体", "group", language));
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
            "Too large: only the first "
                + max
                + " "
                + en
                + " were kept; draft the rest separately."));
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

  /** What one detail call is asked to fill in, with relations, facts and hints as context. */
  private String detailRequest(
      List<Entry> batch, Map<String, Entry> byKey, Map<String, Map<String, String>> facts) {
    ArrayNode request = json.createArrayNode();
    for (var entry : batch) {
      var node = request.addObject();
      node.put("key", entry.key);
      node.put("type", entry.type);
      node.set("name", nameNode(entry.name));
      if (facts.containsKey(entry.key)) node.set("facts", json.valueToTree(facts.get(entry.key)));
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
          problems.add(
              language.pick("AI 没有给出这一项的属性，请手动填写。", "No attributes were drafted; fill them in."));
        } else {
          sanitize(entry.type, detail.path("data"), data, uncertain);
          for (var field : detail.path("uncertain"))
            if (field.isTextual() && !uncertain.contains(field.asText()))
              uncertain.add(field.asText());
        }
        for (var field : uncertain) if (!required(entry.type, field)) data.remove(field);
        // The roster decided the fruit type; the detail call only received it as context.
        if (entry.type.equals(FRUIT) && entry.rosterData.has("fruit_type"))
          data.set("fruit_type", entry.rosterData.get("fruit_type"));
        if (entry.type.equals(CHARACTER)) {
          if (!data.has("hakis")) data.putArray("hakis");
          fitFruitCapacity(entry.fruits.size(), data, problems, language);
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
              if (ImportRevisionScope.PROFILE.contains(key)) {
                var fact =
                    CharacterFactValues.read(Map.of(key, field.getValue().asText())).get(key);
                if (fact == null) {
                  data.remove(key);
                  if (!uncertain.contains(key)) uncertain.add(key);
                } else data.set(key, fact);
                return;
              }
              JsonNode value = repair(field.getValue(), schema);
              if (value == null) {
                if (!uncertain.contains(key)) uncertain.add(key);
                return;
              }
              if (!value.equals(field.getValue()) && !uncertain.contains(key)) uncertain.add(key);
              data.set(key, value);
            });
  }

  private static boolean required(String type, String field) {
    for (var key : ActionSchemas.fields(type, true).path("required"))
      if (key.asText().equals(field)) return true;
    return false;
  }

  private static boolean sameContent(ImportItem a, ImportItem b) {
    return a != null
        && Objects.equals(a.data(), b.data())
        && Objects.equals(a.fruits(), b.fruits())
        && Objects.equals(a.group(), b.group())
        && Objects.equals(a.position(), b.position())
        && new HashSet<>(a.uncertain() == null ? List.of() : a.uncertain())
            .equals(new HashSet<>(b.uncertain() == null ? List.of() : b.uncertain()));
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
        number =
            Math.max(
                schema.path("minimum").asDouble(),
                Math.min(schema.path("maximum").asDouble(), number));
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

  /** A new character holds one devil fruit; only Blackbeard ever held two, and he exists. */
  private void fitFruitCapacity(
      int fruits, ObjectNode data, List<String> problems, AssistantLanguage language) {
    data.put("maximum_fruit_count", 1);
    if (fruits > 1)
      problems.add(
          language.pick(
              "新角色只能吃 1 个果实，请去掉多余的。", "A new character holds one fruit; remove the others."));
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
