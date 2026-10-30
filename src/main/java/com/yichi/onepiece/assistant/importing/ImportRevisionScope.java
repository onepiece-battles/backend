package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.yichi.onepiece.assistant.tools.ActionSchemas;
import java.util.*;
import java.util.regex.Pattern;

/** Bounds a model patch by the CURRENT instruction, not by the model's own claims. */
final class ImportRevisionScope {
  static final Set<String> PROFILE =
      Set.of("bounty", "age", "height", "birth_place", "date_of_birth");
  static final Set<String> COMBAT =
      Set.of("attack", "defense", "blood", "speed", "intelligence", "energy", "group_gain");
  private static final Map<String, String> ALIASES = new LinkedHashMap<>();

  static {
    ALIASES.put("bounty", "悬赏|赏金|bounty");
    ALIASES.put("age", "年龄|岁数|age");
    ALIASES.put("height", "身高|height");
    ALIASES.put("birth_place", "出生地|故乡|birth.?place");
    ALIASES.put("date_of_birth", "生日|出生日期|birthday|date_of_birth");
    ALIASES.put("attack", "攻击|attack");
    ALIASES.put("defense", "防御|防守|defen[cs]e");
    ALIASES.put("blood", "血量|生命|血条|health|blood");
    ALIASES.put("speed", "速度|speed");
    ALIASES.put("intelligence", "智力|intelligence");
    ALIASES.put("energy", "精力|耐力|体力|energy|stamina");
    ALIASES.put("group_gain", "团队加成|团队增益|团战加成|团战增益|group_gain|crew bonus");
    ALIASES.put("race", "种族|race");
    ALIASES.put("battle_style", "战斗风格|战斗方式|battle_style|fighting style");
    ALIASES.put("hakis", "霸气|haki");
    ALIASES.put("name", "名字|名称|姓名|改名|译名|中文名|英文名|rename|\\bname\\b");
    ALIASES.put("fruit_type", "果实类型|果实种类|fruit_type|fruit type");
    ALIASES.put("energy_consume", "精力消耗|能量消耗|energy_consume|energy cost");
    ALIASES.put("group_category", "团体类型|团体种类|阵营|group_category|group type");
    ALIASES.put("position", "职位|职务|船长|副船长|position|captain");
  }

  private static final String CLAUSES = "[，,；;。\\n]|然后|再把|并把";
  private static final String BULK = "全部|所有|每个|每一|一律|大家|全员|整个|统一|\\b(all|every|everyone|each)\\b";
  private static final String CLEAR =
      "留空|清空|空着|不填|不要填|删掉|去掉|未知|不确定|不知道|查不到|\\b(clear|blank|unset|unknown)\\b";
  private static final String UNSURE = "不确定|未知|不明|没把握|查不到|uncertain|unsure|unknown";
  private static final String REMOVE =
      "删|移除|去掉|拿掉|不要|剔除|踢|排除|换成|替换|不是|不属于|不算|"
          + "\\b(remove|delete|drop|exclude|replace|without|not)\\b";
  private static final String ADD =
      "加|添|补|新增|漏|缺|少了|还有|换成|替换|\\b(add|include|missing|replace|also)\\b";
  private static final String RENAME = "叫|称作|写作|写成|翻译|译名|\\b(call|spell|translat)";
  private static final Set<String> RELATIONS = Set.of("fruits", "group", "position");
  private static final Map<String, String> POSITIONS =
      Map.ofEntries(
          Map.entry("captain", "(?<!副)船长|\\bcaptain\\b"),
          Map.entry("first_mate", "副船长|大副|first mate"),
          Map.entry("combatant", "战斗员|干部|combatant"),
          Map.entry("doctor", "船医|医生|doctor"),
          Map.entry("cook", "厨师|cook"),
          Map.entry("navigator", "航海士|navigator"),
          Map.entry("musician", "音乐家|musician"),
          Map.entry("shipwright", "船匠|shipwright"),
          Map.entry("helmsman", "舵手|helmsman"),
          Map.entry("scholar", "考古学家|学者|scholar"));

  private final Map<String, Set<String>> fields = new HashMap<>();
  private final Set<String> removals = new HashSet<>();
  private final Map<String, Set<String>> clears = new HashMap<>();
  private final Map<String, Set<String>> lookUps = new HashMap<>();
  private final Set<String> plannedAdditions = new LinkedHashSet<>();
  boolean additions;
  private boolean planned;
  private String reply;
  private final String instruction;
  private final boolean englishName;
  private final boolean chineseName;

  ImportRevisionScope(List<ImportItem> items, List<Turn> history, String instruction) {
    this(items, history, instruction, null);
  }

  /**
   * With {@code plan}, the model's reading of the request ({@link ImportPrompts#PLAN}), the scope
   * is the part of the plan the request grounds. Without a plan, or when it neither grounds a
   * change nor asks a question, the scope is what the request's keywords name.
   */
  ImportRevisionScope(
      List<ImportItem> items, List<Turn> history, String instruction, JsonNode plan) {
    String request = instruction == null ? "" : instruction;
    this.instruction = request;
    englishName = has(request, "英文|english|en_US");
    chineseName = has(request, "中文|chinese|zh_CN");
    additions =
        has(
                request,
                "添加|加上|新增|补上|补充.*(成员|角色|果实|团体)|补全.*成员|补齐.*成员|换成|替换为|\\b(add|include|replace)\\b")
            && !has(request, "(?:加上|新增|添加)\\s*[0-9.%]");
    Set<String> named = named(items, request);
    boolean bulk = has(request, "全部|所有|每个|每一|一律|\\b(all|every|everyone)\\b");
    boolean rewrite =
        has(request, "重新生成|重新起草|重写|重做|redraft|rewrite")
            && has(request, "全部属性|所有属性|整个草稿|全部草稿|all fields|whole draft");
    Set<String> requested = requestedFields(request);
    if (requested.isEmpty()
        && has(request, "给他|给她|分给|give|assign|transfer")
        && items.stream()
            .anyMatch(i -> i.type().equals(FRUIT) && (named.contains(i.key()) || named.isEmpty())))
      requested.add("fruits");
    if (rewrite)
      for (var item : items) {
        if (bulk || named.contains(item.key()) || named.isEmpty() && items.size() == 1) {
          var all = new HashSet<String>();
          ActionSchemas.fields(item.type(), true)
              .path("properties")
              .fieldNames()
              .forEachRemaining(all::add);
          all.remove("name");
          fields.put(item.key(), all);
        }
      }
    if (!named.isEmpty()) {
      Set<String> previous = Set.of();
      for (String clause : request.split(CLAUSES)) {
        Set<String> targets = named(items, clause);
        Set<String> wanted = requestedFields(clause);
        if (wanted.isEmpty()
            && requested.contains("fruits")
            && has(clause, "给|分给|give|assign|transfer")) wanted.add("fruits");
        if (targets.isEmpty() && !wanted.isEmpty()) targets = previous;
        if (!targets.isEmpty()) previous = targets;
        // Distinct name/field spans prevent "A's attack, B's bounty" crossing targets.
        Set<String> explicitTargets = named(items, clause);
        List<ImportItem> ordered =
            items.stream()
                .filter(i -> explicitTargets.contains(i.key()))
                .sorted(Comparator.comparingInt(i -> mentionIndex(i, clause)))
                .toList();
        if (ordered.size() > 1) {
          for (int i = 0; i < ordered.size(); i++) {
            var item = ordered.get(i);
            int start = mentionIndex(item, clause),
                end =
                    i + 1 < ordered.size()
                        ? mentionIndex(ordered.get(i + 1), clause)
                        : clause.length();
            var own = requestedFields(clause.substring(start, end));
            if (own.isEmpty()
                && (has(clause.substring(start, end), "和|与|及|、|\\band\\b")
                    || wanted.contains("fruits")
                    || wanted.contains("group"))) own = wanted;
            addFields(item, own);
          }
        } else for (var item : items) if (targets.contains(item.key())) addFields(item, wanted);
      }
    } else if (!requested.isEmpty()) {
      Set<String> targets = new HashSet<>();
      if (bulk)
        for (var item : items) {
          if (targetType(item, request)) targets.add(item.key());
        }
      else if (has(request, "不确定|未知|不明|unsure|unknown")) {
        for (var item : items)
          for (String field : requested)
            if (item.uncertain() != null && item.uncertain().contains(field))
              fields.computeIfAbsent(item.key(), k -> new HashSet<>()).add(field);
      } else {
        if (history != null)
          for (int i = history.size() - 1; i >= 0; i--) {
            Turn turn = history.get(i);
            if (turn != null && "user".equals(turn.role())) {
              targets = named(items, turn.content());
              if (!targets.isEmpty()) break;
            }
          }
        targets.removeIf(
            key ->
                items.stream()
                    .noneMatch(i -> i.key().equals(key) && !supported(i, requested).isEmpty()));
        if (targets.size() > 1)
          targets.clear(); // An unresolved "he/she" is not permission to edit everyone.
        if (targets.isEmpty()) {
          var candidates = items.stream().filter(i -> !supported(i, requested).isEmpty()).toList();
          if (candidates.size() == 1) targets.add(candidates.get(0).key());
        }
      }
      for (var item : items) if (targets.contains(item.key())) addFields(item, requested);
    }
    protect(items, request, fields);
    for (String clause : request.split(CLAUSES)) {
      boolean clear = has(clause, "留空|清空|不填|不要填|删掉|去掉|\\b(clear|blank|unset)\\b");
      boolean unknown = has(clause, "未知|\\bunknown\\b") && !has(clause, "查|核实|补|verify|look|find");
      if (!clear && !unknown) continue;
      Set<String> targets = named(items, clause);
      Set<String> wanted = requestedFields(clause);
      for (var item : items)
        if (targets.isEmpty() || targets.contains(item.key()))
          for (String field : wanted)
            if (allows(item, field)) {
              if (has(clause, "不确定的|未知的|unknown facts|unknown values")
                  && (item.uncertain() == null || !item.uncertain().contains(field))) continue;
              clears.computeIfAbsent(item.key(), k -> new HashSet<>()).add(field);
            }
    }
    // Resource removal is different from clearing one of its fields.
    for (var item : items)
      for (String alias : names(item)) {
        String quoted = Pattern.quote(alias);
        if (has(request, "(?:删除|移除|去掉|删掉|不要|remove|delete|drop)\\s*(?:角色|成员|果实|团体)?\\s*" + quoted)
            || has(request, quoted + "\\s*(?:删掉|去掉|不要了)")) removals.add(item.key());
      }
    if (has(request, "换成|替换为|\\breplace\\b") && removals.isEmpty()) {
      var candidates =
          items.stream()
              .filter(
                  i -> i.type().equals(CHARACTER) && (named.isEmpty() || named.contains(i.key())))
              .toList();
      if (candidates.size() == 1) removals.add(candidates.get(0).key());
    }
    // Giving an explicitly named fruit to a new holder also removes it from its old holder.
    if (has(request, "给|分给|转给|交给|give|assign|transfer") && has(request, "果实|fruit")) {
      var namedFruits =
          items.stream()
              .filter(i -> i.type().equals(FRUIT) && named.contains(i.key()))
              .map(ImportItem::key)
              .toList();
      for (var item : items)
        if (item.type().equals(CHARACTER)
            && item.fruits() != null
            && !Collections.disjoint(item.fruits(), namedFruits))
          fields.computeIfAbsent(item.key(), k -> new HashSet<>()).add("fruits");
    }
    if (plan != null && plan.isObject()) planned = plan(items, history, request, plan);
  }

  /** "Only A's attack, don't change B's bounty" must never grant the negative clause. */
  private static void protect(
      List<ImportItem> items, String request, Map<String, Set<String>> granted) {
    for (String clause : request.split(CLAUSES)) {
      if (!has(clause, "不要改|别改|不修改|不要动|别动|保持|不变|do not|don't|unchanged|keep")) continue;
      if (has(clause, "其他|其余|other|rest")) continue;
      Set<String> protectedFields = requestedFields(clause);
      Set<String> protectedItems = named(items, clause);
      for (var item : items)
        if (protectedItems.isEmpty() || protectedItems.contains(item.key())) {
          Set<String> fields = granted.get(item.key());
          if (fields == null) continue;
          if (protectedFields.isEmpty()) fields.clear();
          else fields.removeAll(supported(item, protectedFields));
        }
    }
    granted.values().removeIf(Set::isEmpty);
  }

  /**
   * Replaces the keyword scope with the part of {@code plan} the request grounds, and says whether
   * it did. A target must be one the request refers to ({@link #grounded}); fields the request
   * names for an item bound the plan's fields for it, so "A's attack, B's bounty" cannot cross; a
   * clear needs a word for blank or unknown, a removal a word for taking out, an addition its name
   * or a word for adding. A plan that grounds no change but asks a question (or answers one)
   * leaves nothing to change, and the question is the reply.
   */
  private boolean plan(List<ImportItem> items, List<Turn> history, String request, JsonNode plan) {
    Map<String, ImportItem> byKey = new HashMap<>();
    items.forEach(i -> byKey.put(i.key(), i));
    Set<String> grounded = grounded(items, history, request);
    boolean clearing = has(request, CLEAR);
    boolean unsure = has(request, UNSURE);
    boolean renaming = alias(request, ALIASES.get("name")) || has(request, RENAME);
    Map<String, Set<String>> edits = new HashMap<>(), lookUp = new HashMap<>(), blank = new HashMap<>();
    for (JsonNode target : plan.path("targets")) {
      var item = byKey.get(target.path("key").asText());
      if (item == null) continue;
      Set<String> wanted = new LinkedHashSet<>();
      for (JsonNode field : target.path("fields"))
        if (Set.of("stats", "combat").contains(field.asText())) wanted.addAll(COMBAT);
        else wanted.add(field.asText());
      Set<String> granted = supported(item, wanted);
      if (!renaming) granted.remove("name");
      Set<String> explicit = fields.getOrDefault(item.key(), Set.of());
      if (!explicit.isEmpty()) granted.retainAll(explicit);
      String action = target.path("action").asText();
      for (String field : granted) {
        boolean data = !RELATIONS.contains(field) && !field.equals("name");
        if (action.equals("clear")) {
          // "Leave what is uncertain blank" is grounded by the item's own uncertain list.
          if (data
              && (clearing && grounded.contains(item.key())
                  || unsure && item.uncertain() != null && item.uncertain().contains(field)))
            blank.computeIfAbsent(item.key(), k -> new HashSet<>()).add(field);
        } else if (grounded.contains(item.key()))
          (action.equals("look_up") && data ? lookUp : edits)
              .computeIfAbsent(item.key(), k -> new HashSet<>())
              .add(field);
      }
    }
    Set<String> removed = new LinkedHashSet<>();
    if (has(request, REMOVE))
      for (JsonNode key : plan.path("remove"))
        if (byKey.containsKey(key.asText()) && grounded.contains(key.asText()))
          removed.add(key.asText());
    Set<String> added = new LinkedHashSet<>();
    for (JsonNode add : plan.path("add")) {
      String name = (add.isTextual() ? add.asText() : add.path("name").asText()).trim();
      if (!name.isEmpty() && (mentions(request, name) || has(request, ADD))) added.add(name);
    }
    String question = text(plan.path("question"));
    if (question == null) question = text(plan.path("answer"));
    protect(items, request, edits);
    protect(items, request, lookUp);
    protect(items, request, blank);
    if (edits.isEmpty() && lookUp.isEmpty() && blank.isEmpty() && removed.isEmpty() && added.isEmpty()
        && question == null) return false;
    fields.clear();
    edits.forEach((key, set) -> fields.computeIfAbsent(key, k -> new HashSet<>()).addAll(set));
    lookUp.forEach((key, set) -> fields.computeIfAbsent(key, k -> new HashSet<>()).addAll(set));
    lookUps.putAll(lookUp);
    clears.clear();
    clears.putAll(blank);
    removals.clear();
    removals.addAll(removed);
    plannedAdditions.addAll(added);
    additions = !added.isEmpty();
    reply = question;
    return true;
  }

  /**
   * The items the request refers to: by name; through a named item (a group's members, a
   * character's fruit, a fruit's holders); by position ("the captain"); by an "all" word; or,
   * when it names none ("him", "a bit higher"), the items the conversation named last. When even
   * that finds nothing, the only item of a type in the draft is meant.
   */
  private static Set<String> grounded(List<ImportItem> items, List<Turn> history, String request) {
    Set<String> named = named(items, request);
    Set<String> result = new LinkedHashSet<>(named);
    for (var item : items) {
      if (!item.type().equals(CHARACTER)) continue;
      var fruits = item.fruits() == null ? List.<String>of() : item.fruits();
      if (named.contains(item.key())) result.addAll(fruits);
      if (named.contains(item.group()) || fruits.stream().anyMatch(named::contains))
        result.add(item.key());
      if (item.position() != null
          && POSITIONS.containsKey(item.position())
          && has(request, POSITIONS.get(item.position()))) result.add(item.key());
    }
    if (has(request, BULK)) items.forEach(i -> result.add(i.key()));
    if (named.isEmpty() && history != null)
      for (int i = history.size() - 1; i >= 0; i--) {
        Turn turn = history.get(i);
        if (turn == null || turn.content() == null) continue;
        var mentioned = named(items, turn.content());
        result.addAll(mentioned);
        if ("user".equals(turn.role()) && !mentioned.isEmpty()) break;
      }
    if (result.isEmpty())
      for (String type : List.of(GROUP, FRUIT, CHARACTER)) {
        var ofType = items.stream().filter(i -> type.equals(i.type())).toList();
        if (ofType.size() == 1) result.add(ofType.get(0).key());
      }
    return result;
  }

  private static boolean mentions(String request, String name) {
    if (request.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) return true;
    String last = name.substring(name.lastIndexOf('·') + 1);
    return last.length() >= 2 && !last.equals(name) && request.contains(last);
  }

  private static String text(JsonNode node) {
    return node.isTextual() && !node.asText().isBlank() ? node.asText().trim() : null;
  }

  /**
   * Explicit additions plus their necessary fruit/holder dependencies, never unrelated new items.
   */
  Set<String> addedKeys(JsonNode add) {
    Set<String> allowed = new HashSet<>();
    Map<String, JsonNode> characters = new LinkedHashMap<>();
    Map<String, JsonNode> fruits = new LinkedHashMap<>();
    boolean crewMembers =
        has(instruction, "补齐|补全|全员|所有成员|全部成员|缺少.*成员|漏.*成员|missing members|all members");
    for (String type : List.of(GROUP, FRUIT, CHARACTER))
      for (JsonNode node : add.path(type + "s")) {
        String key = node.path("key").asText();
        if (type.equals(CHARACTER)) characters.put(key, node);
        if (type.equals(FRUIT)) fruits.put(key, node);
        var fake =
            new ImportItem(
                key,
                type,
                node.deepCopy() instanceof com.fasterxml.jackson.databind.node.ObjectNode data
                    ? data
                    : null,
                null,
                List.of(),
                null,
                null,
                List.of(),
                List.of());
        if (mentionIndex(fake, instruction) >= 0
            || crewMembers && type.equals(CHARACTER)
            || names(fake).stream().anyMatch(this::plannedAddition)) allowed.add(key);
      }
    // Adding a canon fruit necessarily needs a holder in this import; adding a character may need
    // their canon fruit. Do not expand to other crew members or groups as an accidental side
    // effect.
    for (var entry : characters.entrySet()) {
      if (!allowed.contains(entry.getKey()))
        for (JsonNode fruit : entry.getValue().path("fruits"))
          if (allowed.contains(fruit.asText()) && fruits.containsKey(fruit.asText())) {
            allowed.add(entry.getKey());
            break;
          }
      if (allowed.contains(entry.getKey()))
        for (JsonNode fruit : entry.getValue().path("fruits"))
          if (fruits.containsKey(fruit.asText())) allowed.add(fruit.asText());
    }
    return allowed;
  }

  private static boolean targetType(ImportItem item, String request) {
    if (has(request, "资源|resource|整个草稿|whole draft")) return true;
    if (has(request, "角色|成员|characters?|members?") && !has(request, "果实|团体|fruits?|groups?"))
      return item.type().equals(CHARACTER);
    if (has(request, "果实|fruits?") && !has(request, "角色|成员|团体|characters?|groups?"))
      return item.type().equals(FRUIT);
    if (has(request, "团体|groups?") && !has(request, "果实|角色|成员|fruits?|characters?"))
      return item.type().equals(GROUP);
    return true;
  }

  private void addFields(ImportItem item, Set<String> wanted) {
    fields.computeIfAbsent(item.key(), k -> new HashSet<>()).addAll(supported(item, wanted));
  }

  private static Set<String> supported(ImportItem item, Set<String> requested) {
    Set<String> result = new HashSet<>();
    JsonNode props = ActionSchemas.fields(item.type(), true).path("properties");
    for (String field : requested) {
      if (props.has(field)
          || (item.type().equals(CHARACTER)
              && Set.of("fruits", "group", "position").contains(field))) result.add(field);
      if (item.type().equals(FRUIT) && COMBAT.contains(field)) {
        if (props.has(field + "_slope")) result.add(field + "_slope");
        if (props.has(field + "_intercept")) result.add(field + "_intercept");
      }
    }
    return result;
  }

  private boolean plannedAddition(String name) {
    String normalized = ExistingResources.normalize(name);
    if (normalized.isEmpty()) return false;
    for (String planned : plannedAdditions) {
      String last = planned.substring(planned.lastIndexOf('·') + 1);
      if (ExistingResources.normalize(planned).equals(normalized)
          || last.length() >= 2 && ExistingResources.normalize(last).equals(normalized)) return true;
    }
    return false;
  }

  /** Whether a model plan, not the keywords, decided this scope. */
  boolean planned() {
    return planned;
  }

  /**
   * Whether the edit call has anything to do: always under the keyword scope; under a plan only
   * for field changes and additions (look-ups, clears and removals need no model).
   */
  boolean needsEdit() {
    return !planned || additions || !describe().isEmpty();
  }

  /** The plan's question or answer, when it asks something back. */
  String reply() {
    return reply;
  }

  /** Fields of items to look up again, by key: the plan's "look_up" targets. */
  Map<String, Set<String>> lookUps() {
    return lookUps;
  }

  boolean looksUp(ImportItem item, String field) {
    return lookUps.getOrDefault(item.key(), Set.of()).contains(field);
  }

  /** Items a plan takes out of the draft; empty under the keyword scope. */
  Set<String> removals() {
    return planned ? removals : Set.of();
  }

  /** What a plan adds, by name. */
  Set<String> plannedAdditions() {
    return plannedAdditions;
  }

  boolean allows(ImportItem item, String field) {
    return fields.getOrDefault(item.key(), Set.of()).contains(field);
  }

  boolean allowsName(String language) {
    return !englishName && !chineseName
        || language.equals("en_US") && englishName
        || language.equals("zh_CN") && chineseName;
  }

  boolean clears(ImportItem item, String field) {
    return clears.getOrDefault(item.key(), Set.of()).contains(field);
  }

  boolean removes(String key) {
    return removals.contains(key);
  }

  List<String> redraft(ImportItem item, JsonNode requested) {
    Set<String> allowed = new LinkedHashSet<>(fields.getOrDefault(item.key(), Set.of()));
    allowed.removeAll(Set.of("name", "fruits", "group", "position"));
    allowed.removeAll(clears.getOrDefault(item.key(), Set.of()));
    if (requested.isArray() && !requested.isEmpty()) {
      Set<String> wanted = new HashSet<>();
      requested.forEach(
          f -> {
            String field = f.asText();
            if (field.equals("stats") || field.equals("combat")) wanted.addAll(COMBAT);
            else wanted.add(field);
          });
      if (!wanted.contains("all")) allowed.retainAll(supported(item, wanted));
    }
    return List.copyOf(allowed);
  }

  /** The fields the edit call may change, by key: without look-ups and clears. */
  Map<String, Set<String>> describe() {
    Map<String, Set<String>> result = new TreeMap<>();
    fields.forEach(
        (key, set) -> {
          Set<String> left = new TreeSet<>(set);
          left.removeAll(lookUps.getOrDefault(key, Set.of()));
          left.removeAll(clears.getOrDefault(key, Set.of()));
          if (!left.isEmpty()) result.put(key, left);
        });
    return result;
  }

  private static Set<String> requestedFields(String text) {
    Set<String> result = new LinkedHashSet<>();
    ALIASES.forEach(
        (field, pattern) -> {
          if (alias(text, pattern)) result.add(field);
        });
    for (String stat : COMBAT)
      for (String suffix : List.of("_slope", "_intercept"))
        if (has(text, "\\b" + stat + suffix + "\\b")) result.add(stat + suffix);
    boolean slope = has(text, "系数|倍率|_slope|multiplier");
    boolean intercept = has(text, "固定加成|固定增益|截距|_intercept|intercept");
    if (slope || intercept)
      for (String stat : COMBAT)
        if (result.remove(stat)) {
          if (slope) result.add(stat + "_slope");
          if (intercept) result.add(stat + "_intercept");
        }
    if (has(text, "战斗属性|战力|战斗力|数值|强一点|弱一点|强一些|弱一些|combat stats|combat power|stats|stronger|weaker"))
      result.addAll(COMBAT);
    if (has(text, "资料|基本信息|档案|profile|facts")
        || has(text, "不确定|未知|unknown") && has(text, "信息|资料|facts")) result.addAll(PROFILE);
    if (has(text, "果实|fruit") && has(text, "给|吃|服用|分配|换|去掉|移除|give|eat|assign|replace|remove"))
      result.add("fruits");
    if (has(text, "加入|入团|所属|归属|退出|移出|离开|join|leave|membership")) result.add("group");
    return result;
  }

  private static Set<String> named(List<ImportItem> items, String text) {
    Set<String> result = new LinkedHashSet<>();
    for (var item : items) if (mentionIndex(item, text) >= 0) result.add(item.key());
    return result;
  }

  private static int mentionIndex(ImportItem item, String text) {
    if (text == null) return -1;
    int first = Integer.MAX_VALUE;
    for (String name : names(item)) {
      var matcher =
          Pattern.compile(
                  name.matches("[A-Za-z0-9 _.-]+")
                      ? "(?<![A-Za-z0-9])" + Pattern.quote(name) + "(?![A-Za-z0-9])"
                      : Pattern.quote(name),
                  Pattern.CASE_INSENSITIVE)
              .matcher(text);
      if (matcher.find()) first = Math.min(first, matcher.start());
    }
    return first == Integer.MAX_VALUE ? -1 : first;
  }

  private static List<String> names(ImportItem item) {
    List<String> result = new ArrayList<>();
    if (item.key() != null) result.add(item.key());
    JsonNode name = item.data() == null ? null : item.data().get("name");
    if (name != null)
      for (String lang : List.of("zh_CN", "en_US")) {
        String value = name.path(lang).asText().trim();
        if (!value.isEmpty()) {
          result.add(value);
          if (lang.equals("zh_CN") && value.contains("·")) {
            String last = value.substring(value.lastIndexOf('·') + 1);
            if (last.length() >= 2) result.add(last);
          }
        }
      }
    return result;
  }

  private static boolean alias(String text, String alternatives) {
    for (String pattern : alternatives.split("\\|")) {
      boolean latin = pattern.codePoints().allMatch(c -> c < 128);
      if (has(text, latin ? "(?<![A-Za-z0-9_])(?:" + pattern + ")(?![A-Za-z0-9_])" : pattern))
        return true;
    }
    return false;
  }

  private static boolean has(String text, String regex) {
    return text != null && Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find();
  }
}
