package com.yichi.onepiece.assistant.tools;

import static com.yichi.onepiece.assistant.tools.ResourceQueryTools.*;
import static com.yichi.onepiece.assistant.tools.ToolJson.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.yichi.onepiece.assistant.agent.*;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.model.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class BattleTools implements AssistantToolProvider {
  private final ResourceLookup lookup;

  public BattleTools(ResourceLookup lookup) {
    this.lookup = lookup;
  }

  @Override
  public List<AssistantTool> tools() {
    var search =
        querySchema(
            """
            {"starter_name":{"type":"string"},"receiver_name":{"type":"string"},
             "statuses":{"type":"array","items":{"type":"string","enum":["CREATED","IN_PROGRESS","FINISHED","FAILED"]}},
             "types":{"type":"array","items":{"type":"string","enum":["SINGLE","GROUP"]}}}
            """);
    ((com.fasterxml.jackson.databind.node.ObjectNode) search.get("properties")).remove("name");
    return List.of(
        tool(
            "search_battles",
            "Paged visible battle records filtered by participants, status, type and scope. Returns"
                + " actual execution and report progress.",
            search,
            this::search,
            "正在查询战斗记录",
            "Searching battles"),
        tool(
            "get_battle_detail",
            "Read a battle by UUID, including participants, result, score changes and report"
                + " progress. Use get_battle_rounds for round details.",
            referenceSchema("battle", false),
            this::detail,
            "正在查询战斗详情",
            "Loading battle details"),
        tool(
            "get_battle_status",
            "Read current battle execution and report-generation status. Never claims a winner"
                + " before the recorded result exists.",
            referenceSchema("battle", false),
            this::detail,
            "正在查询战斗进度",
            "Loading battle progress"),
        tool(
            "get_battle_report",
            "Check whether the battle report is ready and return its file ID and battle page link;"
                + " does not download PDF bytes.",
            referenceSchema("battle", false),
            this::report,
            "正在查询战报",
            "Checking the battle report"),
        tool(
            "get_battle_rounds",
            "Paged recorded attack and status details; no simulated or invented rounds.",
            referenceSchema("battle", true),
            this::rounds,
            "正在查询战斗回合",
            "Loading battle rounds"),
        tool(
            "get_character_battles",
            "Paged battle history for an exact character UUID or unambiguous name, covering both"
                + " starter and receiver roles.",
            referenceSchema("character", true),
            this::history,
            "正在查询角色战斗历史",
            "Loading character battle history"),
        tool(
            "get_head_to_head",
            "Recorded encounters between two characters, including wins and unfinished encounters;"
                + " a summary of recorded history only.",
            schema(
                """
                {"type":"object","properties":{"first":{"type":"string"},"second":{"type":"string"},"page":{"type":"integer","minimum":1},"page_size":{"type":"integer","minimum":1,"maximum":30}},"required":["first","second"],"additionalProperties":false}
                """),
            this::headToHead,
            "正在比较交战记录",
            "Loading head-to-head history"));
  }

  ToolResult search(JsonNode a, ToolContext c) {
    lookup.check(ResourceType.BATTLE, Permission.LIST);
    var filter =
        new BattleFilter()
            .starterName(text(a, "starter_name"))
            .receiverName(text(a, "receiver_name"))
            .languageCode(c.language().languageCode())
            .battleStatuses(enumList(a, "statuses", BattleStatus.class))
            .battleTypes(enumList(a, "types", BattleType.class));
    if (!scope(a).equals("system")) filter.userId(c.userId());
    if (!scope(a).equals("mine")) filter.systemResource(true);
    var response =
        lookup.battles.list(
            new ListBattleRequest()
                .pageIndex(page(a))
                .pageSize(size(a))
                .battleFilter(filter)
                .sortKeys(
                    new ArrayList<>(
                        List.of(
                            new BattleSortableField()
                                .sortKey(BattleSortableFieldsEnum.CREATION_RECORD_ID)
                                .sortOrder(order(a))))),
            scope(a).equals("all_visible"));
    if (response == null) throw problem("lookup_failed", "Battle list unavailable");
    var refs = new ArrayList<ResourceLink>();
    var rows = new ArrayList<Object>();
    for (var value : nonNull(response.getBattles())) rows.add(ToolViews.battle(value, c, refs));
    return result(ToolViews.page("battles", rows, response.getTotalSize(), page(a), size(a)), refs);
  }

  ToolResult detail(JsonNode a, ToolContext c) {
    var value = lookup.battle(requiredText(a, "battle"), c);
    var refs = new ArrayList<ResourceLink>();
    var view = ToolViews.battle(value, c, refs);
    view.put("recorded_attack_count", nonNull(value.getBattleProcessDetails()).size());
    view.put("recorded_status_count", nonNull(value.getBattleStatusDetails()).size());
    return result(view, refs);
  }

  ToolResult report(JsonNode a, ToolContext c) {
    var value = lookup.battle(requiredText(a, "battle"), c);
    var refs = new ArrayList<ResourceLink>();
    var view = ToolViews.battle(value, c, refs);
    var execution = value.getBattleExecution();
    view.put("report_file_id", execution == null ? null : execution.getReportFileId());
    view.put("download_available", execution != null && execution.getReportFileId() != null);
    return result(view, refs);
  }

  ToolResult rounds(JsonNode a, ToolContext c) {
    var value = lookup.battle(requiredText(a, "battle"), c);
    var refs = new ArrayList<ResourceLink>();
    var view = ToolViews.battle(value, c, refs);
    view.put("attacks", slice(nonNull(value.getBattleProcessDetails()), a));
    view.put("statuses", slice(nonNull(value.getBattleStatusDetails()), a));
    view.put("attack_total", nonNull(value.getBattleProcessDetails()).size());
    view.put("status_total", nonNull(value.getBattleStatusDetails()).size());
    view.put("page", page(a));
    view.put("page_size", size(a));
    view.put(
        "has_more",
        (long) page(a) * size(a)
            < Math.max(
                nonNull(value.getBattleProcessDetails()).size(),
                nonNull(value.getBattleStatusDetails()).size()));
    return result(view, refs);
  }

  ToolResult history(JsonNode a, ToolContext c) {
    var character = lookup.character(requiredText(a, "character"), c);
    return historyResult(nonNull(character.getBattles()), a, c);
  }

  ToolResult headToHead(JsonNode a, ToolContext c) {
    var first = lookup.character(requiredText(a, "first"), c);
    var second = lookup.character(requiredText(a, "second"), c);
    if (first.getId().equals(second.getId()))
      throw problem("invalid_arguments", "Choose two distinct characters");
    var encounters =
        nonNull(first.getBattles()).stream()
            .filter(
                b ->
                    (first.getId().equals(b.getStarterId())
                            && second.getId().equals(b.getReceiverId()))
                        || (second.getId().equals(b.getStarterId())
                            && first.getId().equals(b.getReceiverId())))
            .toList();
    var refs = new ArrayList<ResourceLink>();
    var rows = new ArrayList<Object>();
    for (var b : slice(encounters, a)) rows.add(ToolViews.battle(b, c, refs));
    var view = ToolViews.page("battles", rows, encounters.size(), page(a), size(a));
    view.put("first", ToolViews.resource(first, "character", c, refs));
    view.put("second", ToolViews.resource(second, "character", c, refs));
    view.put("first_wins", wins(encounters, first.getId()));
    view.put("second_wins", wins(encounters, second.getId()));
    view.put("draws", encounters.stream().filter(BattleTools::isDraw).count());
    view.put(
        "unresolved",
        encounters.stream()
            .filter(
                b ->
                    !isDraw(b)
                        && (b.getBattleExecution() == null
                            || b.getBattleExecution().getWinnerId() == null))
            .count());
    return result(view, refs);
  }

  /** FAILED marks a draw: the battle reached the turn limit with both sides standing. */
  private static boolean isDraw(BattleListView battle) {
    return battle.getBattleExecution() != null
        && battle.getBattleExecution().getBattleStatus() == BattleStatus.FAILED;
  }

  private long wins(List<BattleListView> values, UUID id) {
    return values.stream()
        .filter(
            b ->
                b.getBattleExecution() != null
                    && b.getBattleExecution().getBattleStatus() == BattleStatus.FINISHED
                    && id.equals(b.getBattleExecution().getWinnerId()))
        .count();
  }

  private ToolResult historyResult(List<BattleListView> values, JsonNode a, ToolContext c) {
    var refs = new ArrayList<ResourceLink>();
    var rows = new ArrayList<Object>();
    for (var b : slice(values, a)) rows.add(ToolViews.battle(b, c, refs));
    return result(ToolViews.page("battles", rows, values.size(), page(a), size(a)), refs);
  }

  static <T> List<T> slice(List<T> values, JsonNode a) {
    int start = (int) Math.min((long) (page(a) - 1) * size(a), values.size());
    return values.subList(start, Math.min(start + size(a), values.size()));
  }
}
