package com.yichi.onepiece.assistant.tools;

import static com.yichi.onepiece.assistant.tools.ToolJson.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.yichi.onepiece.assistant.agent.*;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.battle.service.BattleExecutionAsyncService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.UserHelper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Reuses application services; called only inside the confirmation transaction. */
@Service
public class ResourceActionExecutor {
  private final ResourceLookup lookup;
  private final BattleExecutionAsyncService jobs;
  private final ActionRecordService records;
  private final JdbcTemplate jdbc;
  private static final ObjectMapper JSON =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public ResourceActionExecutor(
      ResourceLookup lookup,
      BattleExecutionAsyncService jobs,
      ActionRecordService records,
      JdbcTemplate jdbc) {
    this.lookup = lookup;
    this.jobs = jobs;
    this.records = records;
    this.jdbc = jdbc;
  }

  static byte[] uuidBytes(String id) {
    UUID uuid = UUID.fromString(id);
    return java.nio.ByteBuffer.allocate(16)
        .putLong(uuid.getMostSignificantBits())
        .putLong(uuid.getLeastSignificantBits())
        .array();
  }

  record Plan(ObjectNode payload, List<ResourceLink> resources, String summary) {}

  Plan plan(String operation, JsonNode arguments, ToolContext context) {
    ActionSchemas.validate(arguments, ActionSchemas.parameters(operation), "arguments");
    ObjectNode input = arguments.deepCopy();
    ObjectNode snapshots = JSON.createObjectNode();
    var refs = new ArrayList<ResourceLink>();
    String primary =
        operation.contains("character")
            ? "character"
            : operation.contains("fruit")
                ? "fruit"
                : operation.contains("group") ? "group" : "battle";
    if (operation.startsWith("create_")) {
      lookup.check(ResourceType.fromValue(primary.toUpperCase(Locale.ROOT)), Permission.CREATE);
      if (!primary.equals("group"))
        lookup.check(ResourceType.fromValue(primary.toUpperCase(Locale.ROOT)), Permission.GET);
      if (operation.equals("create_battle")) {
        var starter = resolve(input, "starter", "character", false, context, snapshots, refs);
        var receiver = resolve(input, "receiver", "character", false, context, snapshots, refs);
        if (starter.path("id").equals(receiver.path("id")))
          throw problem("invalid_arguments", "A character cannot fight itself");
      } else if (operation.equals("create_character") && !input.path("data").has("hakis"))
        ((ObjectNode) input.get("data")).putArray("hakis");
    } else if (operation.startsWith("update_")) {
      var current = resolve(input, primary, primary, true, context, snapshots, refs);
      JsonNode patch = input.get("changes");
      if (primary.equals("character")
          && patch.has("maximum_fruit_count")
          && patch.get("maximum_fruit_count").asInt() < current.path("fruits").size())
        throw problem(
            "invalid_arguments", "Fruit capacity cannot be smaller than the current fruit count");
      if (primary.equals("group")
          && patch.has("group_category")
          && !patch.get("group_category").equals(current.get("group_category"))) {
        if (current.path("characters").size() > 0 || current.path("sub_groups").size() > 0)
          throw problem(
              "invalid_arguments", "Only an empty group can change category through the assistant");
      }
    } else
      switch (operation) {
        case "eat_fruit" -> {
          var character = resolve(input, "character", "character", true, context, snapshots, refs);
          var fruit = resolve(input, "fruit", "fruit", false, context, snapshots, refs);
          if (character.path("fruits").size() >= character.path("maximum_fruit_count").asInt())
            throw problem("fruit_limit", "The character has reached its fruit capacity");
          for (var f : character.path("fruits"))
            if (f.path("id").equals(fruit.path("id")))
              throw problem("duplicate_fruit", "This character already holds the fruit");
        }
        case "join_group" -> {
          resolve(input, "character", "character", true, context, snapshots, refs);
          var group = resolve(input, "group", "group", false, context, snapshots, refs);
          if ("group".equals(group.path("group_category").asText()))
            throw problem(
                "invalid_arguments",
                "A hierarchy container accepts subgroups, not character members");
        }
        case "add_group_members" -> {
          var group = resolve(input, "group", "group", true, context, snapshots, refs);
          if ("group".equals(group.path("group_category").asText()))
            throw problem(
                "invalid_arguments", "A hierarchy container accepts subgroups, not characters");
          resolveArray(input, "characters", "character", context, snapshots, refs);
        }
        case "attach_subgroups" -> {
          var group = resolve(input, "group", "group", true, context, snapshots, refs);
          if (!"group".equals(group.path("group_category").asText()))
            throw problem(
                "invalid_arguments", "The parent must be a hierarchy container (group category)");
          resolveArray(input, "children", "group", context, snapshots, refs);
          for (var child : input.path("children")) {
            if (child.asText().equals(group.path("id").asText()))
              throw problem("invalid_arguments", "A group cannot be its own parent");
            for (var ancestor : group.path("father_groups"))
              if (child.equals(ancestor.get("id")))
                throw problem(
                    "invalid_arguments", "The proposed relationship would create a cycle");
          }
        }
        case "start_battle" -> {
          var battle = resolve(input, "battle", "battle", true, context, snapshots, refs);
          if (!"CREATED".equals(battle.path("battle_execution").path("battle_status").asText()))
            throw problem("invalid_state", "Only a CREATED battle can be started");
          resolveId(
              "character", battle.path("starter_id").asText(), false, context, snapshots, refs);
          resolveId(
              "character", battle.path("receiver_id").asText(), false, context, snapshots, refs);
        }
        default -> throw problem("invalid_arguments", "Unknown action");
      }
    var payload = JSON.createObjectNode();
    payload.set("input", input);
    payload.set("snapshots", snapshots);
    return new Plan(
        payload, List.copyOf(refs), summary(operation, input, snapshots, refs, context));
  }

  private JsonNode resolve(
      ObjectNode input,
      String key,
      String type,
      boolean edit,
      ToolContext c,
      ObjectNode snapshots,
      List<ResourceLink> refs) {
    var value = resolveId(type, requiredText(input, key), edit, c, snapshots, refs);
    input.put(key, value.path("id").asText());
    return value;
  }

  private void resolveArray(
      ObjectNode input,
      String key,
      String type,
      ToolContext c,
      ObjectNode snapshots,
      List<ResourceLink> refs) {
    var normalized = JSON.createArrayNode();
    var unique = new HashSet<String>();
    for (var value : input.path(key)) {
      var resource = resolveId(type, value.asText(), true, c, snapshots, refs);
      String id = resource.path("id").asText();
      if (!unique.add(id)) throw problem("invalid_arguments", "Duplicate resource");
      normalized.add(id);
    }
    input.set(key, normalized);
  }

  private JsonNode resolveId(
      String type,
      String reference,
      boolean edit,
      ToolContext c,
      ObjectNode snapshots,
      List<ResourceLink> refs) {
    var value = read(type, reference, c);
    if (edit) {
      lookup.check(ResourceType.fromValue(type.toUpperCase(Locale.ROOT)), Permission.UPDATE);
      own(value, c);
    }
    if (type.equals("battle")) ToolViews.battle(value, c, refs);
    else ToolViews.resource(value, type, c, refs);
    snapshots.set(type + ":" + value.path("id").asText(), snapshot(type, value));
    return value;
  }

  private JsonNode read(String type, String reference, ToolContext c) {
    Object value =
        switch (type) {
          case "character" -> lookup.character(reference, c);
          case "fruit" -> lookup.fruit(reference, c);
          case "group" -> lookup.group(reference, c);
          case "battle" -> lookup.battle(reference, c);
          default -> throw problem("invalid_arguments", "Unknown type");
        };
    if (value == null) throw problem("not_found", "Resource no longer exists");
    return JSON.valueToTree(value);
  }

  private void own(JsonNode value, ToolContext c) {
    if (UserHelper.hasNikaPermission()) return;
    var actor = UserHelper.getCurrentUserDetail();
    var owner = value.path("creation_record");
    if (!c.userId().toString().equals(owner.path("user_id").asText())
        || !actor.getLoginRoleId().toString().equals(owner.path("role_id").asText()))
      throw problem(
          "permission_denied", "Only NIKA or the resource's creator in the same role may edit it");
  }

  private ObjectNode snapshot(String type, JsonNode value) {
    var node = (ObjectNode) value.deepCopy();
    node.remove(
        List.of(
            "previous_character",
            "next_character",
            "previous_fruit",
            "next_fruit",
            "previous_group",
            "next_group",
            "previous_battle",
            "next_battle",
            "battles",
            "battle_process_details",
            "battle_status_details"));
    // Details are bounded app resources; relations remain part of the stale-change check.
    return node;
  }

  void lockAndCheck(String operation, ObjectNode payload, ToolContext c) {
    var snapshots = payload.path("snapshots");
    var keys = new ArrayList<String>();
    snapshots.fieldNames().forEachRemaining(keys::add);
    Collections.sort(keys);
    if (keys.stream().anyMatch(k -> k.startsWith("group:")))
      jdbc.queryForList("SELECT id FROM group_hierarchy_lock WHERE id=1 FOR UPDATE");
    for (var key : keys) {
      String[] parts = key.split(":", 2);
      String table =
          switch (parts[0]) {
            case "character" -> "`character`";
            case "fruit" -> "fruit";
            case "group" -> "`group`";
            case "battle" -> "battle";
            default -> throw new IllegalStateException("Invalid persisted resource type");
          };
      jdbc.queryForList("SELECT id FROM " + table + " WHERE id=? FOR UPDATE", uuidBytes(parts[1]));
    }
    var fresh = plan(operation, payload.get("input"), c);
    if (!sameSnapshot(fresh.payload().get("snapshots"), snapshots))
      throw problem(
          "stale_action",
          c.language()
              .pick(
                  "资源信息已更新，请重新生成提案后再确认。",
                  "Resource data changed after this proposal. Ask for a fresh proposal before"
                      + " confirming."));
  }

  /**
   * Stored JSON has no Java integer widths: LongNode(1) is read back as IntNode(1). Compare numbers
   * exactly by value at every depth while retaining object fields, string/boolean types and array
   * order (fruit modifier order can affect combat stats).
   */
  static boolean sameSnapshot(JsonNode fresh, JsonNode stored) {
    return fresh.equals(
        (left, right) ->
            left.isNumber() && right.isNumber()
                ? left.decimalValue().compareTo(right.decimalValue())
                : left.toString().compareTo(right.toString()),
        stored);
  }

  ToolResult execute(String operation, ObjectNode payload, ToolContext c) {
    ObjectNode input = (ObjectNode) payload.get("input");
    var refs = new ArrayList<ResourceLink>();
    UUID id;
    String type;
    switch (operation) {
      case "create_character" -> {
        var request = JSON.convertValue(input.get("data"), CreateCharacterRequest.class);
        request
            .name(createdName(input))
            .userId(c.userId())
            .roleId(UserHelper.getCurrentUserDetail().getLoginRoleId());
        var created = lookup.characters.createCharacter(request);
        id = created.getId();
        type = "character";
      }
      case "create_fruit" -> {
        var request = JSON.convertValue(input.get("data"), CreateFruitRequest.class);
        request
            .name(createdName(input))
            .userId(c.userId())
            .roleId(UserHelper.getCurrentUserDetail().getLoginRoleId());
        var created = lookup.fruits.createFruit(request);
        id = created.getId();
        type = "fruit";
      }
      case "create_group" -> {
        var created =
            lookup.groups.create(
                JSON.convertValue(input.get("data"), CreateGroupRequest.class)
                    .name(createdName(input)));
        id = created.getId();
        type = "group";
      }
      case "create_battle" -> {
        var created =
            lookup.battles.createBattle(
                new CreateBattleRequest()
                    .userId(c.userId())
                    .starterId(UUID.fromString(input.path("starter").asText()))
                    .receiverId(UUID.fromString(input.path("receiver").asText())));
        id = created.getBattleId();
        type = "battle";
      }
      case "update_character" -> {
        id = UUID.fromString(input.path("character").asText());
        type = "character";
        var current = (ObjectNode) read(type, id.toString(), c);
        if (!current.path("hakis").isArray()) current.putArray("hakis");
        merge(current, (ObjectNode) input.get("changes"), true);
        lookup.characters.updateCharacter(JSON.convertValue(current, UpdateCharacterRequest.class));
      }
      case "update_fruit" -> {
        id = UUID.fromString(input.path("fruit").asText());
        type = "fruit";
        var current = (ObjectNode) read(type, id.toString(), c);
        merge(current, (ObjectNode) input.get("changes"), false);
        lookup.fruits.updateFruit(JSON.convertValue(current, UpdateFruitRequest.class));
      }
      case "update_group" -> {
        id = UUID.fromString(input.path("group").asText());
        type = "group";
        var current = (ObjectNode) read(type, id.toString(), c);
        merge(current, (ObjectNode) input.get("changes"), false);
        current.put("modification_record_id", records.create().getId());
        lookup.groups.update(JSON.convertValue(current, Group.class));
      }
      case "eat_fruit" -> {
        id = UUID.fromString(input.path("character").asText());
        type = "character";
        lookup.characters.eatFruit(id, UUID.fromString(input.path("fruit").asText()));
      }
      case "join_group" -> {
        id = UUID.fromString(input.path("character").asText());
        type = "character";
        lookup.characters.joinGroup(
            new JoinGroupRequest()
                .characterId(id)
                .groupId(UUID.fromString(input.path("group").asText()))
                .position(GroupPosition.fromValue(input.path("position").asText())));
      }
      case "add_group_members" -> {
        id = UUID.fromString(input.path("group").asText());
        type = "group";
        var response =
            lookup.groups.addCharacter(
                new AddCharacterRequest().groupId(id).characterIds(ids(input.path("characters"))));
        if (!Boolean.TRUE.equals(response.getResult()))
          throw problem("invalid_state", "The group members could not be updated");
      }
      case "attach_subgroups" -> {
        id = UUID.fromString(input.path("group").asText());
        type = "group";
        var response =
            lookup.groups.addGroup(
                new AddGroupRequest().fatherGroupId(id).childGroupIds(ids(input.path("children"))));
        if (!Boolean.TRUE.equals(response.getResult()))
          throw problem("invalid_state", "The group relationship could not be updated");
      }
      case "start_battle" -> {
        id = UUID.fromString(input.path("battle").asText());
        type = "battle";
        jobs.scheduleAsyncJob(id);
      }
      default -> throw problem("invalid_arguments", "Unknown action");
    }
    // The caller already holds GET permission for these projections.
    if (operation.startsWith("create_")) {
      String name =
          type.equals("battle")
              ? c.language().pick("战斗记录", "Battle record")
              : c.language().displayName(createdName(input));
      refs.add(link(type, id, name));
    } else {
      JsonNode value = read(type, id.toString(), c);
      ToolViews.resource(value, type, c, refs);
      if (type.equals("battle")) {
        refs.clear();
        ToolViews.battle(value, c, refs);
      }
    }
    return result(
        map(
            "success",
            true,
            "operation",
            operation,
            "resource_type",
            type,
            "id",
            id,
            "message",
            c.language().pick("操作已完成。", "Operation completed.")),
        refs);
  }

  private static I18nText createdName(ObjectNode input) {
    return ActionSchemas.i18nText(input.path("data").path("name"), new I18nText());
  }

  static void merge(ObjectNode current, ObjectNode changes, boolean character) {
    changes
        .fields()
        .forEachRemaining(
            e -> {
              String key =
                  character && ActionSchemas.STATS.contains(e.getKey())
                      ? "base_" + e.getKey()
                      : e.getKey();
              JsonNode value = e.getValue();
              if (key.equals("name")) {
                // A partial name keeps the current view's other language.
                var name = JSON.convertValue(current.get("name"), I18nText.class);
                value =
                    JSON.valueToTree(
                        ActionSchemas.i18nText(value, name == null ? new I18nText() : name));
              }
              current.set(key, value);
            });
  }

  private List<UUID> ids(JsonNode values) {
    var ids = new ArrayList<UUID>();
    values.forEach(v -> ids.add(UUID.fromString(v.asText())));
    return ids;
  }

  private String summary(
      String op, ObjectNode input, ObjectNode snapshots, List<ResourceLink> refs, ToolContext c) {
    var text =
        new StringBuilder(c.language().pick("待确认操作：", "Proposed action: ")).append(title(op, c));
    if (op.equals("create_battle")) {
      for (String participant : List.of("starter", "receiver")) {
        var ref =
            refs.stream()
                .filter(
                    r ->
                        r.type().equals("character")
                            && r.id().equals(input.path(participant).asText()))
                .findFirst()
                .orElseThrow();
        String role =
            participant.equals("starter")
                ? c.language().pick("发起方", "Starter")
                : c.language().pick("接受方", "Receiver");
        text.append("\n- ")
            .append(role)
            .append(": [")
            .append(ref.name().replace("[", "").replace("]", ""))
            .append("](onepiece://character/")
            .append(ref.id())
            .append(')');
      }
    } else {
      for (var ref : new LinkedHashSet<>(refs))
        text.append("\n- [")
            .append(ref.name().replace("[", "").replace("]", ""))
            .append("](onepiece://")
            .append(ref.type())
            .append('/')
            .append(ref.id())
            .append(')');
    }
    JsonNode values =
        input.has("data") ? input.get("data") : input.has("changes") ? input.get("changes") : input;
    values
        .fields()
        .forEachRemaining(
            e -> {
              if (Set.of(
                      "character",
                      "fruit",
                      "group",
                      "battle",
                      "starter",
                      "receiver",
                      "characters",
                      "children")
                  .contains(e.getKey())) return;
              text.append("\n- ").append(label(e.getKey(), c)).append(": ");
              if (input.has("changes")) {
                JsonNode original = snapshots.elements().next();
                String key =
                    op.equals("update_character") && ActionSchemas.STATS.contains(e.getKey())
                        ? "base_" + e.getKey()
                        : e.getKey();
                text.append(display(original.get(key), c)).append(" → ");
              }
              text.append(display(e.getValue(), c));
            });
    if (Set.of("join_group", "add_group_members").contains(op))
      text.append(
          c.language()
              .pick(
                  "\n这会将角色当前所属团体改为上述团体，原团体保留为历史关系。",
                  "\n"
                      + "This changes the characters' current crew and keeps their previous crew as"
                      + " history."));
    if (op.equals("attach_subgroups"))
      text.append(
          c.language()
              .pick(
                  "\n这些子团体会移动到上述父团体。", "\nThese subgroups will move under the specified parent."));
    if (op.equals("start_battle"))
      text.append(
          c.language()
              .pick(
                  "\n开始后会执行战斗、更新参战角色分数并生成战报。",
                  "\n"
                      + "Starting runs the battle, updates participant scores and generates a"
                      + " report."));
    if (op.equals("update_fruit"))
      text.append(
          c.language()
              .pick(
                  "\n修改系数会重新计算持有者的战斗属性。",
                  "\nCoefficient changes recalculate the holders' combat stats."));
    return text.toString();
  }

  static String title(String op, ToolContext c) {
    String[] zh = {
      "创建角色",
      "修改角色",
      "服用果实",
      "加入团体",
      "创建果实",
      "修改果实",
      "创建团体",
      "修改团体",
      "添加团体成员",
      "调整子团体",
      "创建战斗（尚不开始）",
      "开始战斗并生成战报"
    };
    int i = ActionSchemas.OPERATIONS.indexOf(op);
    return c.language().pick(i < 0 ? op : zh[i], op.replace('_', ' '));
  }

  private String display(JsonNode value, ToolContext c) {
    if (value == null || value.isNull()) return "—";
    if (value.isObject() && (value.has("zh_CN") || value.has("en_US"))) return value.toString();
    if (value.isTextual()) return value.asText().replace('\n', ' ');
    return value.toString();
  }

  private String label(String key, ToolContext c) {
    String[] en = {
      "name",
      "race",
      "battle_style",
      "hakis",
      "age",
      "height",
      "birth_place",
      "date_of_birth",
      "bounty",
      "maximum_fruit_count",
      "group_gain",
      "attack",
      "defense",
      "blood",
      "speed",
      "intelligence",
      "energy",
      "fruit_type",
      "energy_consume",
      "group_category",
      "position"
    };
    String[] zh = {
      "名称", "种族", "战斗风格", "霸气", "年龄", "身高", "出生地", "生日", "悬赏", "果实数量上限", "团队加成", "攻击", "防御", "血量",
      "速度", "智力", "能量", "果实类型", "能量消耗", "团体类型", "职位"
    };
    int i = Arrays.asList(en).indexOf(key);
    if (i >= 0) return c.language().pick(zh[i], key);
    return key;
  }
}
