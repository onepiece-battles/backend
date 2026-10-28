package com.yichi.onepiece.assistant.tools;

import static com.yichi.onepiece.assistant.tools.ToolJson.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.*;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.user.UserHelper;
import java.time.Clock;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Only actual authenticated user text can confirm; deliberately not exposed as an AI tool. */
@Service
public class ActionConfirmationService {
  public static final String PROPOSAL = "action_proposal";
  private static final Pattern COMMAND =
      Pattern.compile(
          "^(确认|取消|confirm|cancel)\\s+([A-Fa-f0-9]{10})[。.!！]?$", Pattern.CASE_INSENSITIVE);
  private static final long TTL = 15 * 60 * 1000L;
  private final JdbcTemplate jdbc;
  private final ResourceActionExecutor executor;
  private final Clock clock;
  private final ObjectMapper json = new ObjectMapper();

  public ActionConfirmationService(
      JdbcTemplate jdbc, ResourceActionExecutor executor, @Qualifier("onepieceClock") Clock clock) {
    this.jdbc = jdbc;
    this.executor = executor;
    this.clock = clock;
  }

  record Pending(
      String code,
      String operation,
      String payload,
      String summary,
      String status,
      long expires,
      String result) {}

  public record Reply(String content, List<ResourceLink> resources) {}

  private String role(ToolContext c) {
    var actor = UserHelper.getCurrentUserDetail();
    if (!c.userId().equals(actor.getId()) || actor.getLoginRoleId() == null)
      throw problem("permission_denied", "User identity changed");
    return actor.getLoginRoleId().toString();
  }

  private List<Pending> rows(String sql, Object... args) {
    return jdbc.query(
        sql,
        (r, n) ->
            new Pending(
                r.getString("code"),
                r.getString("operation"),
                r.getString("payload"),
                r.getString("summary"),
                r.getString("status"),
                r.getLong("expires_at"),
                r.getString("result")),
        args);
  }

  @Transactional
  public ToolResult prepare(
      String operation, com.fasterxml.jackson.databind.JsonNode args, ToolContext c) {
    String role = role(c);
    var plan = executor.plan(operation, args, c);
    // Serialize preparations for one account to bound unfinished proposals even with parallel tool
    // calls.
    jdbc.queryForList(
        "SELECT id FROM `user` WHERE id=? FOR UPDATE",
        ResourceActionExecutor.uuidBytes(c.userId().toString()));
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM assistant_action WHERE user_id=? AND role_id=? AND status IN"
                + " ('PREPARED','PENDING') AND expires_at>?",
            Integer.class,
            c.userId().toString(),
            role,
            clock.millis());
    if (count != null && count >= 8)
      throw problem(
          "pending_limit", "There are already 8 pending changes; confirm or cancel them first");
    String code =
        UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    jdbc.update(
        "INSERT INTO"
            + " assistant_action(code,user_id,role_id,operation,payload,summary,status,created_at,expires_at)"
            + " VALUES(?,?,?,?,?,?,'PREPARED',?,?)",
        code,
        c.userId().toString(),
        role,
        operation,
        plan.payload().toString(),
        plan.summary(),
        clock.millis(),
        clock.millis() + TTL);
    var refs = new ArrayList<>(plan.resources());
    refs.add(new ResourceLink(PROPOSAL, code, plan.summary()));
    return result(
        map(
            "status",
            "awaiting_user_confirmation",
            "operation",
            operation,
            "confirmation_code",
            code,
            "summary",
            plan.summary(),
            "expires_in_seconds",
            TTL / 1000,
            "instructions",
            "No change executed. The server will append the exact confirmation instructions. Never"
                + " claim this action succeeded."),
        refs);
  }

  public ToolResult pending(ToolContext c) {
    var pending =
        rows(
            "SELECT * FROM assistant_action WHERE user_id=? AND role_id=? AND status='PENDING' AND"
                + " expires_at>? ORDER BY created_at",
            c.userId().toString(),
            role(c),
            clock.millis());
    return result(
        map(
            "pending",
            pending.stream()
                .map(p -> map("code", p.code(), "summary", p.summary(), "expires_at", p.expires()))
                .toList()),
        List.of());
  }

  /**
   * Called only for proposals included in a persisted answer. Hidden/failed drafts cannot execute.
   */
  @Transactional
  public void publish(Collection<ResourceLink> links, ToolContext c) {
    for (var link : links)
      if (PROPOSAL.equals(link.type()))
        jdbc.update(
            "UPDATE assistant_action SET status='PENDING' WHERE code=? AND user_id=? AND role_id=?"
                + " AND status='PREPARED' AND expires_at>?",
            link.id(),
            c.userId().toString(),
            role(c),
            clock.millis());
  }

  /** Failed/abandoned answers must not consume the user's pending-proposal allowance. */
  @Transactional
  public void discardUnpublished(Collection<ResourceLink> links, ToolContext c) {
    for (var link : links) {
      if (PROPOSAL.equals(link.type())) {
        jdbc.update(
            "UPDATE assistant_action SET status='CANCELLED' WHERE code=? AND user_id=? AND"
                + " role_id=? AND status='PREPARED'",
            link.id(),
            c.userId().toString(),
            role(c));
      }
    }
  }

  public String instructions(Collection<ResourceLink> links, ToolContext c) {
    StringBuilder text = new StringBuilder();
    for (var link : links)
      if (PROPOSAL.equals(link.type()))
        text.append("\n\n")
            .append(link.name())
            .append(
                c.language()
                    .pick(
                        "\n\n尚未执行。15 分钟内发送 **确认 ",
                        "\n\nNot executed. Within 15 minutes, send **confirm "))
            .append(link.id())
            .append(c.language().pick("** 执行，或 **取消 ", "** to execute, or **cancel "))
            .append(link.id())
            .append(c.language().pick("** 放弃。", "** to discard."));
    return text.toString();
  }

  @Transactional
  public Optional<Reply> handle(String message, ToolContext c) {
    var matcher = COMMAND.matcher(message.trim());
    if (!matcher.matches()) return Optional.empty();
    String command = matcher.group(1).toLowerCase(Locale.ROOT),
        code = matcher.group(2).toUpperCase(Locale.ROOT);
    String role = role(c);
    var found =
        rows(
            "SELECT * FROM assistant_action WHERE code=? AND user_id=? AND role_id=? FOR UPDATE",
            code,
            c.userId().toString(),
            role);
    if (found.isEmpty())
      return Optional.of(
          reply(
              c,
              "没有找到这个待确认操作，请重新提出操作。",
              "No pending action was found. Please request a new proposal."));
    var action = found.get(0);
    if (action.status().equals("DONE")) return Optional.of(decode(action.result()));
    if (action.status().equals("CANCELLED"))
      return Optional.of(reply(c, "这个操作已取消。", "This action was cancelled."));
    if (action.expires() <= clock.millis())
      return Optional.of(
          reply(c, "确认已过期，请重新提出操作。", "This proposal expired. Please request a new one."));
    if (!action.status().equals("PENDING"))
      return Optional.of(
          reply(
              c,
              "这个操作还没有展示完成，请重新提出操作。",
              "This proposal has not been presented. Please request a new one."));
    if (command.equals("取消") || command.equals("cancel")) {
      jdbc.update("UPDATE assistant_action SET status='CANCELLED' WHERE code=?", code);
      return Optional.of(reply(c, "已取消，没有修改数据。", "Cancelled. No data was changed."));
    }
    ObjectNode payload = parse(action.payload());
    executor.lockAndCheck(action.operation(), payload, c);
    ToolResult executed = executor.execute(action.operation(), payload, c);
    StringBuilder content =
        new StringBuilder(c.language().pick("已完成：", "Completed: "))
            .append(ResourceActionExecutor.title(action.operation(), c));
    for (var ref : executed.resources())
      content
          .append("\n- [")
          .append(ref.name().replace("[", "").replace("]", ""))
          .append("](onepiece://")
          .append(ref.type())
          .append('/')
          .append(ref.id())
          .append(')');
    var result = new Reply(content.toString(), executed.resources());
    jdbc.update(
        "UPDATE assistant_action SET status='DONE',result=? WHERE code=?", encode(result), code);
    return Optional.of(result);
  }

  private Reply reply(ToolContext c, String zh, String en) {
    return new Reply(c.language().pick(zh, en), List.of());
  }

  private ObjectNode parse(String value) {
    try {
      return (ObjectNode) json.readTree(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Invalid stored action", e);
    }
  }

  private String encode(Reply value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  private Reply decode(String value) {
    try {
      return json.readValue(value, Reply.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
