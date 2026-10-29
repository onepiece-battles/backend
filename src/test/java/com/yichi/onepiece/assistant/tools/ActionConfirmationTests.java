package com.yichi.onepiece.assistant.tools;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.assistant.agent.*;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.model.UserRole;
import com.yichi.onepiece.testing.ConcurrencyDatabase;
import com.yichi.onepiece.user.model.UserDetailImpl;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testng.annotations.*;

public class ActionConfirmationTests {
  private static final UUID USER = UUID.randomUUID(), ROLE = UUID.randomUUID();
  private final ObjectMapper json = new ObjectMapper();
  private final ToolContext c = new ToolContext(USER, AssistantLanguage.ZH_CN);
  private ConcurrencyDatabase db;
  private JdbcTemplate jdbc;
  private ResourceActionExecutor executor;
  private ActionConfirmationService actions;

  @BeforeMethod
  public void setup() throws Exception {
    db = new ConcurrencyDatabase();
    db.execute("CREATE TABLE `user` (id BINARY(16) PRIMARY KEY)");
    db.execute("CREATE TABLE probe (id INT PRIMARY KEY)");
    db.migrate("V000000030__assistant_action_confirmation.sql");
    jdbc = new JdbcTemplate(db.dataSource());
    jdbc.update("INSERT INTO `user` (id) VALUES (?)", ConcurrencyDatabase.bytes(USER));
    executor = mock(ResourceActionExecutor.class);
    actions =
        db.transactional(
            new ActionConfirmationService(
                jdbc, executor, Clock.fixed(Instant.parse("2026-10-29T00:00:00Z"), ZoneOffset.UTC)),
            ActionConfirmationService.class);
    when(executor.plan(anyString(), any(), any()))
        .thenReturn(
            new ResourceActionExecutor.Plan(
                json.createObjectNode().set("input", json.createObjectNode()),
                List.of(),
                "待确认操作：创建团体\n- 名称：草帽团"));
    when(executor.execute(anyString(), any(), any()))
        .thenReturn(
            new ToolResult(
                "{\"success\":true}",
                List.of(new ResourceLink("group", UUID.randomUUID().toString(), "草帽团"))));
    authenticate(USER, ROLE);
  }

  @AfterMethod
  public void cleanup() throws Exception {
    SecurityContextHolder.clearContext();
    db.close();
  }

  private void authenticate(UUID user, UUID role) {
    var actor = new UserDetailImpl();
    actor.setId(user);
    actor.setLoginRoleId(role);
    actor.setLoginRole(UserRole.NORMAL);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, List.of()));
  }

  private List<ResourceLink> draft() throws Exception {
    return actions.prepare("create_group", json.readTree("{}"), c).resources();
  }

  private String code(List<ResourceLink> refs) {
    return refs.stream()
        .filter(r -> r.type().equals(ActionConfirmationService.PROPOSAL))
        .findFirst()
        .orElseThrow()
        .id();
  }

  private String presented() throws Exception {
    var refs = draft();
    actions.publish(refs, c);
    return code(refs);
  }

  @Test
  public void preparationNeverExecutesAndUnpresentedProposalCannotConfirm() throws Exception {
    var refs = draft();
    String code = code(refs);
    assertThat(actions.instructions(refs, c)).contains("确认 " + code, "取消 " + code, "15 分钟");
    assertThat(actions.handle("确认 " + code, c).orElseThrow().content()).contains("还没有展示");
    verify(executor, never()).execute(anyString(), any(), any());
  }

  @Test
  public void discardingFailedAnswersReleasesOnlyUnpublishedDrafts() throws Exception {
    var hidden = draft();
    String shown = presented();
    actions.discardUnpublished(hidden, c);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM assistant_action WHERE code=?", String.class, code(hidden)))
        .isEqualTo("CANCELLED");
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM assistant_action WHERE code=?", String.class, shown))
        .isEqualTo("PENDING");
  }

  @Test
  public void restoresOldTextOnlyProposalsAndTheirLatestResultFromHistory() throws Exception {
    var refs = draft();
    actions.publish(refs, c);
    String code = code(refs);
    var message =
        new com.yichi.onepiece.assistant.dto.AssistantDto.AiMessageView(
            "old-answer", "assistant", "保留角色对比" + actions.instructions(refs, c), List.of());
    var proposal = actions.decorateMessages(List.of(message), c).get(0).actionProposals().get(0);
    assertThat(proposal.code()).isEqualTo(code);
    assertThat(proposal.status()).isEqualTo("pending");
    assertThat(proposal.summary()).contains("草帽团");
    actions.handle("确认 " + code, c);
    var completed = actions.decorateMessages(List.of(message), c).get(0).actionProposals().get(0);
    assertThat(completed.status()).isEqualTo("completed");
    assertThat(completed.resources()).hasSize(1);
    assertThat(completed.resources().get(0).type()).isEqualTo("group");
    authenticate(USER, UUID.randomUUID());
    assertThat(actions.decorateMessages(List.of(message), c).get(0).actionProposals()).isEmpty();
  }

  @Test
  public void cardProjectionRejectsFabricatedAndUnpublishedCodesAndShowsExpiry() throws Exception {
    var refs = draft();
    assertThat(actions.proposals(refs, c)).isEmpty();
    assertThat(actions.proposalsForText("确认 0123456789", c)).isEmpty();
    actions.publish(refs, c);
    jdbc.update("UPDATE assistant_action SET expires_at=0 WHERE code=?", code(refs));
    assertThat(actions.proposals(refs, c).get(0).status()).isEqualTo("expired");
    var userMessage =
        new com.yichi.onepiece.assistant.dto.AssistantDto.AiMessageView(
            "user", "user", "确认 " + code(refs), List.of());
    assertThat(actions.decorateMessages(List.of(userMessage), c).get(0).actionProposals())
        .isEmpty();
  }

  @Test
  public void onlySeparateExactUserCommandsAreRecognized() throws Exception {
    String code = presented();
    assertThat(actions.handle("请解释‘确认 " + code + "’的含义", c)).isEmpty();
    assertThat(actions.handle("确认", c)).isEmpty();
    assertThat(actions.handle("confirm " + code + " and create another group", c)).isEmpty();
    verify(executor, never()).execute(anyString(), any(), any());
  }

  @Test
  public void confirmationIsDurableAndIdempotent() throws Exception {
    String code = presented();
    var first = actions.handle("确认 " + code, c).orElseThrow();
    var second = actions.handle("confirm " + code.toLowerCase(Locale.ROOT), c).orElseThrow();
    assertThat(first).isEqualTo(second);
    assertThat(first.content()).contains("已完成", "onepiece://group/");
    verify(executor, times(1)).lockAndCheck(eq("create_group"), any(), eq(c));
    verify(executor, times(1)).execute(eq("create_group"), any(), eq(c));
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM assistant_action WHERE code=?", String.class, code))
        .isEqualTo("DONE");
  }

  @Test
  public void codeIsBoundToBothAccountAndLoginRole() throws Exception {
    String code = presented();
    UUID other = UUID.randomUUID();
    authenticate(other, ROLE);
    assertThat(
            actions
                .handle("确认 " + code, new ToolContext(other, AssistantLanguage.ZH_CN))
                .orElseThrow()
                .content())
        .contains("没有找到");
    authenticate(USER, UUID.randomUUID());
    assertThat(actions.handle("确认 " + code, c).orElseThrow().content()).contains("没有找到");
    verify(executor, never()).execute(anyString(), any(), any());
  }

  @Test
  public void cancelAndExpiryNeverMutateResources() throws Exception {
    String cancelled = presented();
    assertThat(actions.handle("取消 " + cancelled, c).orElseThrow().content()).contains("已取消");
    assertThat(actions.handle("确认 " + cancelled, c).orElseThrow().content()).contains("已取消");
    String expired = presented();
    jdbc.update("UPDATE assistant_action SET expires_at=0 WHERE code=?", expired);
    assertThat(actions.handle("确认 " + expired, c).orElseThrow().content()).contains("过期");
    verify(executor, never()).execute(anyString(), any(), any());
  }

  @Test
  public void changedDataOrRevokedPermissionsCannotExecute() throws Exception {
    String code = presented();
    doThrow(ToolJson.problem("stale_action", "Resource changed"))
        .when(executor)
        .lockAndCheck(anyString(), any(), any());
    assertThatThrownBy(() -> actions.handle("确认 " + code, c)).isInstanceOf(ToolProblem.class);
    verify(executor, never()).execute(anyString(), any(), any());
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM assistant_action WHERE code=?", String.class, code))
        .isEqualTo("PENDING");
  }

  @Test
  public void serviceFailureRollsBackResourceChangesAndConfirmation() throws Exception {
    String code = presented();
    when(executor.execute(anyString(), any(), any()))
        .thenAnswer(
            i -> {
              jdbc.update("INSERT INTO probe VALUES (1)");
              throw new IllegalStateException("failed halfway");
            });
    assertThatThrownBy(() -> actions.handle("确认 " + code, c))
        .isInstanceOf(IllegalStateException.class);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM probe", Integer.class)).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM assistant_action WHERE code=?", String.class, code))
        .isEqualTo("PENDING");
  }

  @Test
  public void concurrentConfirmationsExecuteExactlyOnce() throws Exception {
    String code = presented();
    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      Callable<ActionConfirmationService.Reply> command =
          () -> {
            authenticate(USER, ROLE);
            try {
              start.await();
              return actions.handle("确认 " + code, c).orElseThrow();
            } finally {
              SecurityContextHolder.clearContext();
            }
          };
      var first = pool.submit(command);
      var second = pool.submit(command);
      start.countDown();
      assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(second.get(10, TimeUnit.SECONDS));
      verify(executor, times(1)).execute(anyString(), any(), any());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  public void pendingListHidesOtherRolesAndUnpublishedDrafts() throws Exception {
    String code = presented();
    draft();
    assertThat(actions.pending(c).content()).contains(code);
    authenticate(USER, UUID.randomUUID());
    assertThat(actions.pending(c).content()).doesNotContain(code);
  }
}
