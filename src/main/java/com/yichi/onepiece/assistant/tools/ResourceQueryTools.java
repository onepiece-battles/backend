package com.yichi.onepiece.assistant.tools;

import static com.yichi.onepiece.assistant.tools.ToolJson.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.*;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.model.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Paged resource queries, precise details, relationships and non-mutating comparisons. */
@Component
public class ResourceQueryTools implements AssistantToolProvider {
  private final ResourceLookup lookup;

  public ResourceQueryTools(ResourceLookup lookup) {
    this.lookup = lookup;
  }

  static ObjectNode querySchema(String extras) {
    return schema(
            """
            {"type":"object","properties":{
              "name":{"type":"string"},
              "scope":{"type":"string","enum":["all_visible","mine","system"]},
              "page":{"type":"integer","minimum":1},"page_size":{"type":"integer","minimum":1,"maximum":30},
              "sort_order":{"type":"string","enum":["ASC","DESC"]}
            },"additionalProperties":false}
            """)
        .set("properties", mergeProperties(extras));
  }

  private static ObjectNode mergeProperties(String extras) {
    ObjectNode props =
        schema(
            """
            {"name":{"type":"string"},"scope":{"type":"string","enum":["all_visible","mine","system"]},
             "page":{"type":"integer","minimum":1},"page_size":{"type":"integer","minimum":1,"maximum":30},
             "sort_order":{"type":"string","enum":["ASC","DESC"]}}
            """);
    props.setAll(schema(extras));
    return props;
  }

  static AssistantTool tool(
      String name,
      String description,
      ObjectNode params,
      AssistantTool.Handler handler,
      String zh,
      String en) {
    return new AssistantTool(name, description, params, handler, (a, l) -> l.pick(zh, en));
  }

  static ObjectNode referenceSchema(String field, boolean paging) {
    ObjectNode result =
        schema(
            "{\"type\":\"object\",\"properties\":{},\"required\":[\""
                + field
                + "\"],\"additionalProperties\":false}");
    ObjectNode props = (ObjectNode) result.get("properties");
    props.set(
        field,
        schema(
            "{\"type\":\"string\",\"description\":\"Exact name or UUID returned by a resource"
                + " tool\"}"));
    if (paging) {
      props.set("page", schema("{\"type\":\"integer\",\"minimum\":1}"));
      props.set("page_size", schema("{\"type\":\"integer\",\"minimum\":1,\"maximum\":30}"));
    }
    return result;
  }

  @Override
  public List<AssistantTool> tools() {
    ObjectNode characterQuery =
        querySchema(
            """
            {"races":{"type":"array","items":{"type":"string"}},"battle_styles":{"type":"array","items":{"type":"string"}},
             "hakis":{"type":"array","items":{"type":"string"}},"has_fruit":{"type":"boolean"},
             "sort_by":{"type":"string","enum":["name","score","attack","defense","intelligence","blood","speed","group_gain"]}}
            """);
    ObjectNode fruitQuery =
        querySchema(
            """
            {"types":{"type":"array","items":{"type":"string"}},"sort_by":{"type":"string","enum":["name","fruit_type"]}}
            """);
    ObjectNode groupQuery =
        querySchema(
            """
            {"categories":{"type":"array","items":{"type":"string"}},"only_roots":{"type":"boolean"},
             "sort_by":{"type":"string","enum":["name"]}}
            """);
    ObjectNode compare =
        schema(
            """
            {"type":"object","properties":{"characters":{"type":"array","minItems":2,"maxItems":4,"items":{"type":"string"}}},
             "required":["characters"],"additionalProperties":false}
            """);
    ObjectNode tree = referenceSchema("group", false);
    ((ObjectNode) tree.get("properties"))
        .set("max_depth", schema("{\"type\":\"integer\",\"minimum\":0,\"maximum\":4}"));
    ObjectNode preview =
        schema(
            """
            {"type":"object","properties":{"character":{"type":"string"},"fruit":{"type":"string"}},
             "required":["character","fruit"],"additionalProperties":false}
            """);
    return List.of(
        tool(
            "query_characters",
            "Paged character search with combined race, haki, battle-style, fruit-ownership and"
                + " scope filters. Reports total_count; use get_resource_action_schema for enum"
                + " values.",
            characterQuery,
            this::characters,
            "正在筛选角色",
            "Filtering characters"),
        tool(
            "query_fruits",
            "Paged devil-fruit search by names, types and scope, with holder links and"
                + " total_count.",
            fruitQuery,
            this::fruits,
            "正在筛选恶魔果实",
            "Filtering devil fruits"),
        tool(
            "query_groups",
            "Paged crew/group search by name, categories, root-only and scope filters, with"
                + " total_count.",
            groupQuery,
            this::groups,
            "正在筛选团体",
            "Filtering groups"),
        tool(
            "get_character_profile",
            "Resolve a character by UUID or unambiguous name; return base and effective stats,"
                + " fruits, groups and battle counts.",
            referenceSchema("character", false),
            this::characterProfile,
            "正在查询角色完整资料",
            "Loading the character profile"),
        tool(
            "get_character_fruits",
            "List a character's consumed devil fruits and awakening state.",
            referenceSchema("character", false),
            this::characterFruits,
            "正在查询角色的果实",
            "Loading the character's fruits"),
        tool(
            "get_character_groups",
            "List a character's current and former crews, with position and group links.",
            referenceSchema("character", false),
            this::characterGroups,
            "正在查询角色所属团体",
            "Loading the character's crews"),
        tool(
            "get_fruit_detail",
            "Read all fruit coefficients, energy cost, type, holders and awakening state by UUID or"
                + " name.",
            referenceSchema("fruit", false),
            (a, c) -> {
              var refs = new ArrayList<ResourceLink>();
              return result(
                  ToolViews.fruit(lookup.fruit(requiredText(a, "fruit"), c), c, refs, true), refs);
            },
            "正在查询果实完整属性",
            "Loading fruit coefficients and holders"),
        tool(
            "compare_characters",
            "Compare 2-4 existing characters using actual profiles and stats; does not create or"
                + " run a battle.",
            compare,
            this::compare,
            "正在比较角色",
            "Comparing characters"),
        tool(
            "preview_eat_fruit",
            "Read-only preview of adding one fruit using the app's coefficient formula and limits;"
                + " never changes the character.",
            preview,
            this::previewFruit,
            "正在预览果实加成",
            "Previewing fruit effects"),
        tool(
            "get_group_members",
            "Paged direct character members with true character IDs and positions (not relationship"
                + " IDs).",
            referenceSchema("group", true),
            this::members,
            "正在查询团体成员",
            "Loading crew members"),
        tool(
            "get_group_hierarchy",
            "Read a crew's ancestor chain and a bounded child-group tree, up to depth 4 and 50"
                + " nodes. Marks truncated branches.",
            tree,
            this::hierarchy,
            "正在查询团体层级",
            "Loading the group hierarchy"),
        tool(
            "count_resources",
            "Count visible, own, or system characters/fruits/groups/battles using real list totals,"
                + " not a truncated page.",
            schema(
                "{\"type\":\"object\",\"properties\":{\"scope\":{\"type\":\"string\",\"enum\":[\"all_visible\",\"mine\",\"system\"]}},\"additionalProperties\":false}"),
            this::counts,
            "正在统计资源",
            "Counting resources"));
  }

  static String scope(JsonNode args) {
    String value = text(args, "scope");
    if (value.isBlank()) return "all_visible";
    if (!Set.of("all_visible", "mine", "system").contains(value))
      throw problem("invalid_arguments", "Unknown scope");
    return value;
  }

  static int page(JsonNode a) {
    return boundedInt(a, "page", 1, 1, 100000);
  }

  static int size(JsonNode a) {
    return boundedInt(a, "page_size", 10, 1, 30);
  }

  static SortOrder order(JsonNode a) {
    SortOrder value = enumValue(a.get("sort_order"), SortOrder.class, "sort_order");
    return value == null ? SortOrder.DESC : value;
  }

  ToolResult characters(JsonNode a, ToolContext c) {
    lookup.check(ResourceType.CHARACTER, Permission.LIST);
    String scope = scope(a);
    var filter =
        new CharacterFilter()
            .name(text(a, "name"))
            .languageCode(c.language().languageCode())
            .races(enumList(a, "races", Race.class))
            .battleStyles(enumList(a, "battle_styles", BattleStyle.class))
            .hakis(enumList(a, "hakis", Haki.class))
            .hasFruit(bool(a, "has_fruit"));
    if (!scope.equals("system")) filter.userId(c.userId());
    if (!scope.equals("mine")) filter.systemResource(true);
    String sort = text(a, "sort_by");
    if (sort.isBlank()) sort = "score";
    if (!Set.of(
            "name", "score", "attack", "defense", "intelligence", "blood", "speed", "group_gain")
        .contains(sort)) throw problem("invalid_arguments", "Unsupported character sort");
    var response =
        lookup.characters.listCharacters(
            new ListCharacterRequest()
                .pageIndex(page(a))
                .pageSize(size(a))
                .characterFilter(filter)
                .sortKeys(
                    new ArrayList<>(
                        List.of(
                            new CharacterSortableField()
                                .sortKey(CharacterSortableFieldsEnum.fromValue(sort))
                                .sortOrder(order(a))))),
            scope.equals("all_visible"));
    if (response == null) throw problem("lookup_failed", "Character list unavailable");
    var refs = new ArrayList<ResourceLink>();
    var rows = new ArrayList<Object>();
    for (var value : nonNull(response.getCharacters()))
      rows.add(ToolViews.character(value, c, refs));
    return result(
        ToolViews.page("characters", rows, response.getTotalSize(), page(a), size(a)), refs);
  }

  ToolResult fruits(JsonNode a, ToolContext c) {
    lookup.check(ResourceType.FRUIT, Permission.LIST);
    var filter = new FruitFilter().name(text(a, "name")).languageCode(c.language().languageCode());
    String scope = scope(a);
    if (!scope.equals("system")) filter.userId(c.userId());
    if (!scope.equals("mine")) filter.systemResource(true);
    if (a.has("types")) {
      if (!a.get("types").isArray() || a.get("types").size() > 6)
        throw problem("invalid_arguments", "types must be an array");
      var types = new ArrayList<FruitType>();
      for (var raw : a.get("types")) {
        FruitType type = FruitTools.parseFruitType(raw.asText());
        if (type == null) throw problem("invalid_arguments", "Unknown fruit type");
        types.add(type);
      }
      filter.types(types);
    }
    String sort = text(a, "sort_by");
    if (sort.isBlank()) sort = "name";
    if (!Set.of("name", "fruit_type").contains(sort))
      throw problem("invalid_arguments", "Unsupported fruit sort");
    var response =
        lookup.fruits.listFruit(
            new ListFruitRequest()
                .pageIndex(page(a))
                .pageSize(size(a))
                .fruitFilter(filter)
                .sortKeys(
                    new ArrayList<>(
                        List.of(
                            new FruitSortableField()
                                .sortKey(FruitSortableFieldsEnum.fromValue(sort))
                                .sortOrder(order(a))))),
            scope.equals("all_visible"));
    if (response == null) throw problem("lookup_failed", "Fruit list unavailable");
    var refs = new ArrayList<ResourceLink>();
    var rows = new ArrayList<Object>();
    for (var value : nonNull(response.getFruits()))
      rows.add(ToolViews.fruit(value, c, refs, false));
    return result(ToolViews.page("fruits", rows, response.getTotalSize(), page(a), size(a)), refs);
  }

  ToolResult groups(JsonNode a, ToolContext c) {
    lookup.check(ResourceType.GROUP, Permission.LIST);
    var filter =
        new GroupFilter()
            .name(text(a, "name"))
            .languageCode(c.language().languageCode())
            .groupCategories(enumList(a, "categories", GroupCategory.class))
            .noFather(Boolean.TRUE.equals(bool(a, "only_roots")) ? true : null);
    String scope = scope(a);
    if (!scope.equals("system")) filter.userId(c.userId());
    if (!scope.equals("mine")) filter.systemResource(true);
    String sort = text(a, "sort_by");
    if (sort.isBlank()) sort = "name";
    if (!Set.of("name").contains(sort))
      throw problem("invalid_arguments", "Unsupported group sort");
    var response =
        lookup.groups.list(
            new ListGroupRequest()
                .pageIndex(page(a))
                .pageSize(size(a))
                .groupFilter(filter)
                .sortKeys(
                    new ArrayList<>(
                        List.of(
                            new GroupSortableField()
                                .sortKey(GroupSortableFieldsEnum.fromValue(sort))
                                .sortOrder(order(a))))),
            scope.equals("all_visible"));
    if (response == null) throw problem("lookup_failed", "Group list unavailable");
    var refs = new ArrayList<ResourceLink>();
    var rows = new ArrayList<Object>();
    for (var value : nonNull(response.getGroups())) rows.add(ToolViews.group(value, c, refs));
    return result(ToolViews.page("groups", rows, response.getTotalSize(), page(a), size(a)), refs);
  }

  ToolResult characterProfile(JsonNode a, ToolContext c) {
    var value = lookup.character(requiredText(a, "character"), c);
    var refs = new ArrayList<ResourceLink>();
    var view = ToolViews.character(value, c, refs);
    view.put(
        "base_stats",
        ToolViews.fields(
            value,
            "base_attack",
            "base_defense",
            "base_intelligence",
            "base_blood",
            "base_speed",
            "base_group_gain"));
    view.put("battle_count", nonNull(value.getBattles()).size());
    view.put("fruit_count", nonNull(value.getFruits()).size());
    view.put("group_count", nonNull(value.getGroups()).size());
    return result(view, refs);
  }

  ToolResult characterFruits(JsonNode a, ToolContext c) {
    var value = lookup.character(requiredText(a, "character"), c);
    var refs = new ArrayList<ResourceLink>();
    var subject = ToolViews.resource(value, "character", c, refs);
    var rows = new ArrayList<Object>();
    for (var fruit : nonNull(value.getFruits())) {
      var row = ToolViews.fruit(fruit, c, refs, true);
      var relation =
          nonNull(value.getFruitInCharacter()).stream()
              .filter(f -> fruit.getId().equals(f.getFruitId()))
              .findFirst();
      row.put("wake_up", relation.map(FruitInCharacter::getWakeUp).orElse(null));
      rows.add(row);
    }
    return result(
        map(
            "character",
            subject,
            "maximum_fruit_count",
            value.getMaximumFruitCount(),
            "fruits",
            rows),
        refs);
  }

  ToolResult characterGroups(JsonNode a, ToolContext c) {
    var value = lookup.character(requiredText(a, "character"), c);
    var refs = new ArrayList<ResourceLink>();
    var subject = ToolViews.resource(value, "character", c, refs);
    var rows = new ArrayList<Object>();
    for (var crew : nonNull(value.getGroups())) {
      String name = c.language().displayName(crew.getName());
      if (crew.getGroupId() != null) refs.add(link("group", crew.getGroupId(), name));
      rows.add(
          map(
              "id",
              crew.getGroupId(),
              "name",
              name,
              "position",
              crew.getPosition(),
              "former",
              crew.getOnceBelongedTo()));
    }
    return result(map("character", subject, "groups", rows), refs);
  }

  ToolResult compare(JsonNode a, ToolContext c) {
    JsonNode values = a.get("characters");
    if (values == null || !values.isArray() || values.size() < 2 || values.size() > 4)
      throw problem("invalid_arguments", "Compare 2-4 characters");
    var refs = new ArrayList<ResourceLink>();
    var rows = new ArrayList<Object>();
    Set<UUID> ids = new HashSet<>();
    for (var ref : values) {
      var value = lookup.character(ref.asText(), c);
      if (!ids.add(value.getId())) throw problem("invalid_arguments", "Choose distinct characters");
      var row = ToolViews.character(value, c, refs);
      row.put("fruit_count", nonNull(value.getFruits()).size());
      row.put("recorded_battles", nonNull(value.getBattles()).size());
      rows.add(row);
    }
    return result(map("characters", rows, "creates_battle", false), refs);
  }

  ToolResult members(JsonNode a, ToolContext c) {
    var value = lookup.group(requiredText(a, "group"), c);
    var refs = new ArrayList<ResourceLink>();
    var subject = ToolViews.resource(value, "group", c, refs);
    var source = nonNull(value.getCharacters());
    int start = (int) Math.min(source.size(), (long) (page(a) - 1) * size(a));
    var rows = new ArrayList<Object>();
    for (var member : source.subList(start, Math.min(source.size(), start + size(a)))) {
      String name = c.language().displayName(member.getName());
      if (member.getCharacterId() != null)
        refs.add(link("character", member.getCharacterId(), name));
      rows.add(
          map(
              "id",
              member.getCharacterId(),
              "name",
              name,
              "position",
              member.getPosition(),
              "former",
              member.getOnceBelongedTo()));
    }
    var result = ToolViews.page("members", rows, source.size(), page(a), size(a));
    result.put("group", subject);
    return result(result, refs);
  }

  ToolResult hierarchy(JsonNode a, ToolContext c) {
    lookup.check(ResourceType.GROUP, Permission.LIST, Permission.GET);
    lookup.check(ResourceType.CHARACTER, Permission.GET);
    var root = lookup.group(requiredText(a, "group"), c);
    var refs = new ArrayList<ResourceLink>();
    int maxDepth = boundedInt(a, "max_depth", 2, 0, 4);
    var nodes = new ArrayList<Object>();
    var ancestors = new ArrayList<Object>();
    for (var parent : nonNull(root.getFatherGroups()))
      ancestors.add(ToolViews.group(parent, c, refs));
    record Node(Group value, int depth) {}
    Deque<Node> queue = new ArrayDeque<>();
    Group self =
        new Group()
            .id(root.getId())
            .name(root.getName())
            .groupCategory(root.getGroupCategory())
            .fatherGroupId(root.getFatherGroupId());
    queue.add(new Node(self, 0));
    Set<UUID> visited = new HashSet<>();
    boolean truncated = false;
    while (!queue.isEmpty() && nodes.size() < 50) {
      Node next = queue.removeFirst();
      if (!visited.add(next.value().getId())) {
        truncated = true;
        continue;
      }
      var row = ToolViews.group(next.value(), c, refs);
      row.put("depth", next.depth());
      nodes.add(row);
      var response =
          lookup.groups.listChildGroups(new ListChildGroupsRequest().groupId(next.value().getId()));
      var children = response == null ? List.<Group>of() : nonNull(response.getChildGroups());
      if (next.depth() >= maxDepth) {
        if (!children.isEmpty()) {
          row.put("children_truncated", true);
          truncated = true;
        }
        continue;
      }
      for (var child : children) {
        if (nodes.size() + queue.size() >= 50) {
          truncated = true;
          break;
        }
        queue.addLast(new Node(child, next.depth() + 1));
      }
    }
    return result(
        map("ancestors", ancestors, "nodes", nodes, "truncated", truncated || !queue.isEmpty()),
        refs);
  }

  ToolResult counts(JsonNode a, ToolContext c) {
    ObjectNode one = ((ObjectNode) a).deepCopy();
    one.put("page_size", 1);
    var totals = new LinkedHashMap<String, Object>();
    for (var entry :
        Map.of("characters", characters(one, c), "fruits", fruits(one, c), "groups", groups(one, c))
            .entrySet()) {
      try {
        totals.put(
            entry.getKey(), ToolViews.JSON.readTree(entry.getValue().content()).get("total_count"));
      } catch (Exception e) {
        throw new IllegalStateException(e);
      }
    }
    lookup.check(ResourceType.BATTLE, Permission.LIST);
    var filter = new BattleFilter();
    if (!scope(a).equals("system")) filter.userId(c.userId());
    if (!scope(a).equals("mine")) filter.systemResource(true);
    var battles =
        lookup.battles.list(
            new ListBattleRequest()
                .pageIndex(1)
                .pageSize(1)
                .sortKeys(new ArrayList<>())
                .battleFilter(filter),
            scope(a).equals("all_visible"));
    if (battles == null) throw problem("lookup_failed", "Battle list unavailable");
    totals.put("battles", battles.getTotalSize());
    return result(map("scope", scope(a), "totals", totals), List.of());
  }

  ToolResult previewFruit(JsonNode a, ToolContext c) {
    var character = lookup.character(requiredText(a, "character"), c);
    var fruit = lookup.fruit(requiredText(a, "fruit"), c);
    var refs = new ArrayList<ResourceLink>();
    var subject = ToolViews.resource(character, "character", c, refs);
    var candidate = ToolViews.resource(fruit, "fruit", c, refs);
    if (nonNull(character.getFruits()).stream().anyMatch(f -> fruit.getId().equals(f.getId())))
      throw problem("invalid_arguments", "Character already holds this fruit");
    boolean canEat =
        character.getMaximumFruitCount() != null
            && nonNull(character.getFruits()).size() < character.getMaximumFruitCount();
    String[] keys = {"attack", "defense", "blood", "speed", "intelligence", "group_gain"};
    int[] mins = {10, 10, 100, 10, 50, 80};
    int[] maxs = {1500, 1400, 18000, 300, 300, 300};
    JsonNode source = ToolViews.JSON.valueToTree(character);
    var projected = new LinkedHashMap<String, Object>();
    List<JsonNode> modifiers = new ArrayList<>();
    for (var existing : nonNull(character.getFruits()))
      modifiers.add(ToolViews.JSON.valueToTree(existing));
    modifiers.add(ToolViews.JSON.valueToTree(fruit));
    for (int i = 0; i < keys.length; i++) {
      JsonNode base = source.get("base_" + keys[i]);
      if (base == null || !base.isNumber())
        throw problem("incomplete_data", "Character base stats are missing");
      int value = base.asInt();
      for (JsonNode modifier : modifiers) {
        JsonNode slope = modifier.get(keys[i] + "_slope"),
            intercept = modifier.get(keys[i] + "_intercept");
        if (slope == null || !slope.isNumber() || intercept == null || !intercept.isNumber())
          throw problem("incomplete_data", "Fruit coefficients are missing");
        value =
            Math.max(
                mins[i],
                Math.min(maxs[i], (int) (value * slope.asDouble() + intercept.asDouble())));
      }
      projected.put(keys[i], value);
    }
    return result(
        map(
            "simulation_only",
            true,
            "character",
            subject,
            "fruit",
            candidate,
            "capacity_available",
            canEat,
            "current_stats",
            ToolViews.fields(character, keys),
            "projected_stats",
            projected),
        refs);
  }
}
