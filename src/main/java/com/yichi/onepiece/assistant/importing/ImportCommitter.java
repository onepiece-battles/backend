package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.AssistantLanguage;
import com.yichi.onepiece.assistant.agent.ToolProblem;
import com.yichi.onepiece.assistant.tools.ActionSchemas;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports a reviewed draft. The whole batch is validated first and every problem is reported at
 * once; only a clean batch is written, in one transaction: groups, fruits, characters, then the
 * fruits they eat and the groups they join. Existing resources are reused, never changed, except
 * that an existing character can eat a drafted fruit or join a drafted group.
 */
@Service
public class ImportCommitter {
  static final int MAX_ITEMS = 60;
  private static final ObjectMapper JSON =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final CharacterService characters;
  private final FruitService fruits;
  private final GroupService groups;
  private final IamService iam;
  private final ExistingResources existing;

  public ImportCommitter(
      CharacterService characters,
      FruitService fruits,
      GroupService groups,
      IamService iam,
      ExistingResources existing) {
    this.characters = characters;
    this.fruits = fruits;
    this.groups = groups;
    this.iam = iam;
    this.existing = existing;
  }

  /** The batch was not imported; nothing was written. */
  public static final class ImportRejected extends RuntimeException {
    private final List<Problem> problems;

    ImportRejected(List<Problem> problems) {
      super("Import rejected: " + problems.size() + " problem(s)");
      this.problems = List.copyOf(problems);
    }

    public List<Problem> problems() {
      return problems;
    }
  }

  /** What validation learned about an existing resource. */
  private record Current(
      String category, int fruitCapacity, Set<UUID> fruits, UUID currentGroup) {}

  @Transactional
  public CommitResult commit(
      List<ImportItem> items, UUID userId, UUID roleId, AssistantLanguage language) {
    var problems = new ArrayList<Problem>();
    if (items == null || items.isEmpty()) {
      throw new ImportRejected(
          List.of(new Problem(null, language.pick("没有要导入的内容。", "Nothing to import."))));
    }
    if (items.size() > MAX_ITEMS) {
      throw new ImportRejected(
          List.of(
              new Problem(
                  null,
                  language.pick(
                      "一次最多导入 " + MAX_ITEMS + " 项。", "Import at most " + MAX_ITEMS + " items at once."))));
    }
    checkPermissions(items);

    var byKey = new LinkedHashMap<String, ImportItem>();
    var current = new HashMap<String, Current>();
    var names = new HashSet<String>();
    for (var item : items) {
      String key = item.key() == null ? "" : item.key().trim();
      if (key.isEmpty() || byKey.containsKey(key)) {
        problems.add(new Problem(item.key(), language.pick("编号重复或为空。", "Duplicate or empty key.")));
        continue;
      }
      if (!Set.of(CHARACTER, FRUIT, GROUP).contains(item.type())) {
        problems.add(new Problem(key, language.pick("未知类型。", "Unknown type.")));
        continue;
      }
      byKey.put(key, item);
      if (item.existingId() != null) {
        var found = loadExisting(item, language, problems);
        if (found != null) current.put(key, found);
      } else validateNew(item, key, userId, names, language, problems);
    }
    for (var item : byKey.values()) validateRelations(item, byKey, current, language, problems);
    if (!problems.isEmpty()) throw new ImportRejected(problems);

    var ids = new HashMap<String, UUID>();
    var created = new ArrayList<CreatedResource>();
    int reused = 0;
    for (var item : byKey.values())
      if (item.existingId() != null) {
        ids.put(item.key(), UUID.fromString(item.existingId()));
        reused++;
      }
    for (String type : List.of(GROUP, FRUIT, CHARACTER)) {
      for (var item : byKey.values()) {
        if (!item.type().equals(type) || item.existingId() != null) continue;
        UUID id = create(item, userId, roleId);
        ids.put(item.key(), id);
        created.add(
            new CreatedResource(
                item.key(), type, id.toString(), language.displayName(name(item.data()))));
      }
    }

    int eaten = 0, joined = 0;
    for (var item : byKey.values()) {
      if (!item.type().equals(CHARACTER)) continue;
      UUID characterId = ids.get(item.key());
      var now = current.get(item.key());
      for (String fruitKey : nonNull(item.fruits())) {
        UUID fruitId = ids.get(fruitKey);
        if (now != null && now.fruits().contains(fruitId)) continue;
        characters.eatFruit(characterId, fruitId);
        eaten++;
      }
      if (item.group() != null) {
        UUID groupId = ids.get(item.group());
        if (now != null && groupId.equals(now.currentGroup())) continue;
        characters.joinGroup(
            new JoinGroupRequest()
                .characterId(characterId)
                .groupId(groupId)
                .position(GroupPosition.fromValue(position(item))));
        joined++;
      }
    }
    return new CommitResult(created, reused, eaten, joined);
  }

  private void checkPermissions(List<ImportItem> items) {
    var types = new HashSet<String>();
    items.forEach(item -> types.add(item.type()));
    if (types.contains(CHARACTER))
      iam.checkPermissionOfCurrentAccount(
          ResourceType.CHARACTER, Set.of(Permission.CREATE, Permission.GET, Permission.UPDATE));
    if (types.contains(FRUIT))
      iam.checkPermissionOfCurrentAccount(
          ResourceType.FRUIT, Set.of(Permission.CREATE, Permission.GET));
    if (types.contains(GROUP))
      iam.checkPermissionOfCurrentAccount(
          ResourceType.GROUP, Set.of(Permission.CREATE, Permission.GET));
  }

  private Current loadExisting(ImportItem item, AssistantLanguage language, List<Problem> problems) {
    UUID id = uuidOrNull(item.existingId());
    if (id == null) {
      problems.add(new Problem(item.key(), language.pick("已有资源的编号无效。", "Invalid existing id.")));
      return null;
    }
    try {
      return switch (item.type()) {
        case CHARACTER -> {
          var view = characters.getCharacterFullView(id);
          var held = new HashSet<UUID>();
          nonNull(view.getFruits()).forEach(f -> held.add(f.getId()));
          UUID group =
              nonNull(view.getGroups()).stream()
                  .filter(g -> !Boolean.TRUE.equals(g.getOnceBelongedTo()))
                  .map(GroupForCharacterView::getGroupId)
                  .findFirst()
                  .orElse(null);
          yield new Current(null, nullToZero(view.getMaximumFruitCount()), held, group);
        }
        case FRUIT -> {
          fruits.getFruit(new GetFruitRequest().fruitId(id));
          yield new Current(null, 0, Set.of(), null);
        }
        default -> {
          var view = groups.getGroupFullView(new GetGroupRequest().id(id));
          yield new Current(
              view.getGroupCategory() == null ? null : view.getGroupCategory().toString(),
              0,
              Set.of(),
              null);
        }
      };
    } catch (NoSuchElementException e) {
      problems.add(
          new Problem(item.key(), language.pick("这个已有资源已经不存在了。", "This existing resource no longer exists.")));
      return null;
    }
  }

  private void validateNew(
      ImportItem item,
      String key,
      UUID userId,
      Set<String> names,
      AssistantLanguage language,
      List<Problem> problems) {
    ObjectNode data = item.data();
    if (data == null) {
      problems.add(new Problem(key, language.pick("缺少属性。", "Missing attributes.")));
      return;
    }
    if (item.type().equals(CHARACTER) && !data.has("hakis")) data.putArray("hakis");
    try {
      ActionSchemas.validate(data, ActionSchemas.fields(item.type(), true), "data");
    } catch (ToolProblem e) {
      problems.add(new Problem(key, e.getMessage()));
      return;
    }
    var name = name(data);
    for (String text : new String[] {name.getZhCN(), name.getEnUS()}) {
      String normalized = ExistingResources.normalize(text);
      if (!normalized.isEmpty() && !names.add(item.type() + ":" + normalized)) {
        problems.add(
            new Problem(key, language.pick("和本批次另一项重名。", "Same name as another item in this batch.")));
        return;
      }
    }
    if (existing.find(item.type(), name, userId).isPresent())
      problems.add(
          new Problem(
              key,
              language.pick(
                  "已经存在同名资源，请重新起草以复用它。",
                  "A resource with this name already exists; draft again to reuse it.")));
  }

  private void validateRelations(
      ImportItem item,
      Map<String, ImportItem> byKey,
      Map<String, Current> current,
      AssistantLanguage language,
      List<Problem> problems) {
    var itemFruits = nonNull(item.fruits());
    if (!item.type().equals(CHARACTER)) {
      if (!itemFruits.isEmpty() || item.group() != null)
        problems.add(
            new Problem(item.key(), language.pick("只有角色可以吃果实或加入团体。", "Only characters eat fruits or join groups.")));
      return;
    }
    for (String fruitKey : itemFruits) {
      var fruit = byKey.get(fruitKey);
      if (fruit == null || !fruit.type().equals(FRUIT))
        problems.add(new Problem(item.key(), language.pick("引用了不存在的果实：", "Unknown fruit: ") + fruitKey));
    }
    if (new HashSet<>(itemFruits).size() != itemFruits.size())
      problems.add(new Problem(item.key(), language.pick("同一个果实出现了两次。", "The same fruit is listed twice.")));
    var now = current.get(item.key());
    if (item.existingId() != null && now == null) return; // already reported as missing
    int capacity =
        now != null
            ? now.fruitCapacity()
            : item.data() == null ? 0 : item.data().path("maximum_fruit_count").asInt(0);
    int total = now == null ? 0 : now.fruits().size();
    for (String fruitKey : itemFruits) {
      var fruit = byKey.get(fruitKey);
      UUID fruitId = fruit == null ? null : uuidOrNull(fruit.existingId());
      if (now == null || fruitId == null || !now.fruits().contains(fruitId)) total++;
    }
    if (total > capacity)
      problems.add(
          new Problem(
              item.key(),
              language.pick(
                  "果实数量超过上限（" + capacity + "）。",
                  "More fruits than the fruit capacity (" + capacity + ").")));

    if (item.group() != null) {
      var group = byKey.get(item.group());
      if (group == null || !group.type().equals(GROUP)) {
        problems.add(new Problem(item.key(), language.pick("引用了不存在的团体：", "Unknown group: ") + item.group()));
        return;
      }
      String category =
          group.existingId() != null
              ? Optional.ofNullable(current.get(group.key())).map(Current::category).orElse(null)
              : group.data() == null ? null : group.data().path("group_category").asText(null);
      if ("group".equals(category))
        problems.add(
            new Problem(
                item.key(),
                language.pick("层级容器团体不能直接添加角色。", "A hierarchy container cannot have character members.")));
      try {
        GroupPosition.fromValue(position(item));
      } catch (IllegalArgumentException e) {
        problems.add(new Problem(item.key(), language.pick("未知职位：", "Unknown position: ") + item.position()));
      }
    }
  }

  private static UUID uuidOrNull(String value) {
    try {
      return value == null ? null : UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static String position(ImportItem item) {
    return item.position() == null || item.position().isBlank() ? "member" : item.position();
  }

  /**
   * The draft's name as I18nText. Read by hand: the schema's keys are zh_CN / en_US while
   * I18nText's JSON properties are zh-CN / en-US, so convertValue would silently drop the name.
   */
  static I18nText name(JsonNode data) {
    JsonNode name = data.path("name");
    return new I18nText()
        .zhCN(name.hasNonNull("zh_CN") ? name.get("zh_CN").asText() : null)
        .enUS(name.hasNonNull("en_US") ? name.get("en_US").asText() : null);
  }

  private UUID create(ImportItem item, UUID userId, UUID roleId) {
    JsonNode data = item.data();
    I18nText name = name(data);
    return switch (item.type()) {
      case GROUP ->
          groups
              .create(
                  new CreateGroupRequest()
                      .name(name)
                      .groupCategory(GroupCategory.fromValue(data.path("group_category").asText())))
              .getId();
      case FRUIT ->
          fruits
              .createFruit(
                  JSON.convertValue(data, CreateFruitRequest.class)
                      .name(name)
                      .userId(userId)
                      .roleId(roleId))
              .getId();
      default ->
          characters
              .createCharacter(
                  JSON.convertValue(data, CreateCharacterRequest.class)
                      .name(name)
                      .userId(userId)
                      .roleId(roleId))
              .getId();
    };
  }

  private static <T> List<T> nonNull(List<T> values) {
    return values == null ? List.of() : values;
  }

  private static int nullToZero(Integer value) {
    return value == null ? 0 : value;
  }
}
