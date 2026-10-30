package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.yichi.onepiece.assistant.tools.ActionSchemas;
import com.yichi.onepiece.model.I18nText;
import java.util.*;
import java.util.regex.Pattern;

/**
 * What a revision may touch, read from the reviewer's CURRENT request. DeepSeek works out what the
 * request means and finds the values ({@link ImportPrompts#REVISE}); this only keeps its answer
 * to the items the request refers to, to the fields it names when it is about one item, and away
 * from what it says to leave alone, so nothing the reviewer did not mention changes.
 */
final class ImportRevisionScope {
  static final Set<String> PROFILE =
      Set.of("bounty", "age", "height", "birth_place", "date_of_birth");
  static final Set<String> COMBAT =
      Set.of("attack", "defense", "blood", "speed", "intelligence", "energy", "group_gain");
  static final Set<String> RELATIONS = Set.of("fruits", "group", "position");
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
    ALIASES.put(
        "name", "名字|名称|姓名|改名|译名|中文名|英文名|真名|本名|原名|全名|真实姓名|rename|\\bname\\b");
    ALIASES.put("fruit_type", "果实类型|果实种类|fruit_type|fruit type");
    ALIASES.put("energy_consume", "精力消耗|能量消耗|energy_consume|energy cost");
    ALIASES.put("group_category", "团体类型|团体种类|阵营|group_category|group type");
    ALIASES.put("position", "职位|职务|船长|副船长|position|captain");
  }

  private static final String CLAUSES = "[，,；;。\\n]|然后|再把|并把";
  private static final String KEEP =
      "不要改|别改|不修改|不要动|别动|不动|保持|不变|\\b(do not|don't|unchanged|keep)\\b";
  private static final String BULK =
      "全部|所有|每个|每一|一律|大家|全员|整个|统一|\\b(all|every|everyone|each)\\b";
  private static final String PRONOUN =
      "他|她|它|他们|她们|它们|这个|那个|这位|那位|此人|\\b(he|she|him|her|it|they|them|this|that)\\b";
  private static final String ADD =
      "加|添|补|新增|漏|缺|少了|还有|换成|替换|\\b(add|include|missing|replace|also)\\b";
  private static final String PORTRAIT =
      "头像|肖像|照片|图片|画像|\\b(avatar|portrait|photo|picture|image)s?\\b";
  /** Name parts too common to tie a request to one item ("果实攻击都高一点"). */
  private static final Set<String> GENERIC = Set.of("果实", "海贼", "贼团", "一伙", "家族");

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

  private final List<ImportItem> items;
  private final String request;
  private final Set<String> named;
  private final Set<String> grounded;
  private final Set<String> fieldsNamed = new LinkedHashSet<>();
  private final Map<String, Set<String>> kept = new HashMap<>();
  private final boolean portrait;
  private final boolean englishName;
  private final boolean chineseName;

  ImportRevisionScope(List<ImportItem> items, List<Turn> history, String instruction) {
    this.items = items;
    request = instruction == null ? "" : instruction;
    named = named(items, request);
    grounded = grounded(items, history, request, named);
    portrait = asksForPortrait(request);
    englishName = has(request, "英文|english|en_US");
    chineseName = has(request, "中文|chinese|zh_CN");
    // "Only A's attack, don't change B's bounty": a negative clause protects what it names.
    for (String clause : request.split(CLAUSES)) {
      if (!has(clause, KEEP)) {
        fieldsNamed.addAll(requestedFields(clause));
        continue;
      }
      if (has(clause, "其他|其余|别的|\\b(other|rest)\\b")) continue;
      Set<String> fields = requestedFields(clause);
      Set<String> who = named(items, clause);
      if (fields.isEmpty() && who.isEmpty()) continue;
      for (var item : items)
        if (who.isEmpty() || who.contains(item.key()))
          kept.computeIfAbsent(item.key(), k -> new HashSet<>())
              .addAll(fields.isEmpty() ? Set.of("*") : supported(item, fields));
    }
  }

  /**
   * Whether the request refers to the item: by name or as {@link #grounded} finds it, or by the
   * words {@code refersTo} the model quotes from it for this item ({@link #referred}).
   */
  boolean reaches(ImportItem item, JsonNode refersTo) {
    return grounded.contains(item.key()) || referred(refersTo, item);
  }

  /**
   * Whether the answer may change this field of the item: not one a negative clause protects;
   * only relations of an item already in the app; a portrait only when the request asks for a new
   * picture; and when the request is about one item (or none) and names fields, only those. In a
   * request about several items, which field is whose is the model's reading.
   */
  boolean allows(ImportItem item, String field) {
    var keep = kept.getOrDefault(item.key(), Set.of());
    if (keep.contains("*") || keep.contains(field)) return false;
    if (field.equals("avatar"))
      return portrait && item.type().equals(CHARACTER) && item.existingId() == null;
    if (item.existingId() != null && !(item.type().equals(CHARACTER) && RELATIONS.contains(field)))
      return false;
    if (named.size() <= 1 && !fieldsNamed.isEmpty())
      return supported(item, fieldsNamed).contains(field);
    return true;
  }

  /** Whether a rename may touch this language: the one the request names ("英文名"), else both. */
  boolean allowsName(String language) {
    if (englishName == chineseName) return true;
    return language.equals("en_US") ? englishName : chineseName;
  }

  /** Whether the request asks to add something: names it, or uses a word for adding. */
  boolean allowsAddition(I18nText name) {
    return has(request, ADD)
        || name.getZhCN() != null && mentions(name.getZhCN())
        || name.getEnUS() != null && mentions(name.getEnUS());
  }

  /**
   * The items the request refers to: by name; through a named item (a group's members, a
   * character's fruit, a fruit's holders); by position ("the captain"); by an "all" word. When it
   * names none, by part of a name (假路飞 for 冒牌路飞) or else ("him", "a bit higher", "也一样")
   * the items the conversation named last. When even that finds nothing, the only item of a type
   * in the draft is meant.
   */
  private static Set<String> grounded(
      List<ImportItem> items, List<Turn> history, String request, Set<String> named) {
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
    Set<String> partly = named.isEmpty() ? partlyNamed(items, request) : Set.of();
    result.addAll(partly);
    if (named.isEmpty() && partly.isEmpty() && history != null)
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

  /**
   * Whether the request's own {@code words} that the model says mean {@code item} are in the
   * request and do not name another item: a nickname (鹰眼, 黑胡子) or a description (那个女的,
   * 其他人) the names cannot match. A pronoun or a field word ("攻击") is not enough.
   */
  private boolean referred(JsonNode words, ImportItem item) {
    String text = words != null && words.isTextual() ? words.asText().trim() : "";
    if (text.length() < 2 || !request.contains(text)) return false;
    // A pronoun must reach the item through the conversation; a field word is no reference.
    if (has(text, "^(?:" + PRONOUN + ")$") || !requestedFields(text).isEmpty()) return false;
    Set<String> others = named(items, text);
    return others.isEmpty() || others.contains(item.key());
  }

  /** Items with two consecutive Chinese characters of their name in the request. */
  private static Set<String> partlyNamed(List<ImportItem> items, String request) {
    Set<String> result = new LinkedHashSet<>();
    for (var item : items) {
      String name =
          item.data() == null
              ? ""
              : item.data().path("name").path("zh_CN").asText().replaceAll("[\\s·・•.]", "");
      for (int i = 0; i + 2 <= name.length(); i++) {
        String part = name.substring(i, i + 2);
        if (part.codePoints()
                .allMatch(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN)
            && !GENERIC.contains(part)
            && request.contains(part)) {
          result.add(item.key());
          break;
        }
      }
    }
    return result;
  }

  /** Whether some clause asks for a new picture ("换个头像"), not to keep one ("头像别动"). */
  private static boolean asksForPortrait(String request) {
    for (String clause : request.split(CLAUSES))
      if (has(clause, PORTRAIT)
          && !has(clause, "不要|别|不用|保留|不变|不动|不改|\\b(keep|don't|do not|unchanged)\\b"))
        return true;
    return false;
  }

  private boolean mentions(String name) {
    if (request.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT))) return true;
    String last = name.substring(name.lastIndexOf('·') + 1);
    return last.length() >= 2 && !last.equals(name) && request.contains(last);
  }

  private static Set<String> supported(ImportItem item, Set<String> requested) {
    Set<String> result = new HashSet<>();
    JsonNode props = ActionSchemas.fields(item.type(), true).path("properties");
    for (String field : requested) {
      if (props.has(field) || item.type().equals(CHARACTER) && RELATIONS.contains(field))
        result.add(field);
      if (item.type().equals(FRUIT) && COMBAT.contains(field)) {
        if (props.has(field + "_slope")) result.add(field + "_slope");
        if (props.has(field + "_intercept")) result.add(field + "_intercept");
      }
    }
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
    if (has(
        text,
        "战斗属性|战力|战斗力|数值|强一点|弱一点|强一些|弱一些|combat stats|combat power|stats|stronger|weaker"))
      result.addAll(COMBAT);
    if (has(text, "资料|基本信息|档案|profile|facts")) result.addAll(PROFILE);
    if (has(text, "果实|fruit") && has(text, "给|吃|服用|分配|换|去掉|移除|give|eat|assign|replace|remove"))
      result.add("fruits");
    if (has(text, "加入|入团|所属|归属|退出|移出|离开|join|leave|membership")) result.add("group");
    return result;
  }

  private static Set<String> named(List<ImportItem> items, String text) {
    Set<String> result = new LinkedHashSet<>();
    for (var item : items) if (mentioned(item, text)) result.add(item.key());
    return result;
  }

  private static boolean mentioned(ImportItem item, String text) {
    if (text == null) return false;
    for (String name : names(item))
      if (Pattern.compile(
              name.matches("[A-Za-z0-9 _.-]+")
                  ? "(?<![A-Za-z0-9])" + Pattern.quote(name) + "(?![A-Za-z0-9])"
                  : Pattern.quote(name),
              Pattern.CASE_INSENSITIVE)
          .matcher(text)
          .find()) return true;
    return false;
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
