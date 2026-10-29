package com.yichi.onepiece.assistant.tools;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.assistant.agent.*;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.battle.service.*;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.testing.ConcurrencyDatabase;
import com.yichi.onepiece.user.model.UserDetailImpl;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testng.annotations.*;

/**
 * Real plan + JSON database persistence + real snapshot verification, with a stub battle writer.
 */
public class ResourceActionConfirmationRoundTripTests {
  private final UUID user = UUID.randomUUID(),
      role = UUID.randomUUID(),
      starter = UUID.randomUUID(),
      receiver = UUID.randomUUID(),
      battle = UUID.randomUUID();
  private final ToolContext context = new ToolContext(user, AssistantLanguage.EN_US);
  private final ObjectMapper json = new ObjectMapper();
  private ConcurrencyDatabase db;
  private JdbcTemplate jdbc;
  private BattleService battles;
  private CharacterFullView starterView;
  private ActionConfirmationService actions;

  @BeforeMethod
  public void setup() throws Exception {
    db = new ConcurrencyDatabase();
    db.execute("CREATE TABLE `user` (id BINARY(16) PRIMARY KEY)");
    db.execute("CREATE TABLE `character` (id BINARY(16) PRIMARY KEY)");
    db.execute("CREATE TABLE battle_write_probe (id BINARY(16) PRIMARY KEY)");
    db.migrate("V000000030__assistant_action_confirmation.sql");
    jdbc = new JdbcTemplate(db.dataSource());
    jdbc.update("INSERT INTO `user` VALUES (?)", ConcurrencyDatabase.bytes(user));
    jdbc.update("INSERT INTO `character` VALUES (?)", ConcurrencyDatabase.bytes(starter));
    jdbc.update("INSERT INTO `character` VALUES (?)", ConcurrencyDatabase.bytes(receiver));
    var actor = new UserDetailImpl();
    actor.setId(user);
    actor.setLoginRoleId(role);
    actor.setLoginRole(UserRole.NORMAL);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, List.of()));
    var characters = mock(CharacterService.class);
    starterView = profile(starter, "Kizaru", 1L, 3000000000L);
    when(characters.getCharacterFullView(starter)).thenReturn(starterView);
    when(characters.getCharacterFullView(receiver))
        .thenReturn(profile(receiver, "Marco", 2L, 1374000000L));
    battles = mock(BattleService.class);
    when(battles.createBattle(any()))
        .thenAnswer(
            invocation -> {
              CreateBattleRequest request = invocation.getArgument(0);
              assertThat(request.getUserId()).isEqualTo(user);
              assertThat(request.getStarterId()).isEqualTo(starter);
              assertThat(request.getReceiverId()).isEqualTo(receiver);
              jdbc.update(
                  "INSERT INTO battle_write_probe VALUES (?)", ConcurrencyDatabase.bytes(battle));
              return new CreateBattleResponse().battleId(battle).result(true);
            });
    var lookup =
        new ResourceLookup(
            characters,
            mock(FruitService.class),
            mock(GroupService.class),
            battles,
            mock(IamService.class));
    var executor =
        new ResourceActionExecutor(
            lookup, mock(BattleExecutionAsyncService.class), mock(ActionRecordService.class), jdbc);
    actions =
        db.transactional(
            new ActionConfirmationService(
                jdbc, executor, Clock.fixed(Instant.parse("2026-10-29T00:00:00Z"), ZoneOffset.UTC)),
            ActionConfirmationService.class);
  }

  private CharacterFullView profile(UUID id, String name, long record, long bounty) {
    return new CharacterFullView()
        .id(id)
        .name(new I18nText().enUS(name))
        .attack(100)
        .baseAttack(100)
        .bounty(bounty)
        .creationRecord(
            new ActionRecord()
                .id(record)
                .userId(user)
                .roleId(role)
                .actionTimestamp(1780000000000L));
  }

  private List<ResourceLink> propose() throws Exception {
    var prepared =
        actions.prepare(
            "create_battle",
            json.readTree("{\"starter\":\"" + starter + "\",\"receiver\":\"" + receiver + "\"}"),
            context);
    actions.publish(prepared.resources(), context);
    return prepared.resources();
  }

  private String code(List<ResourceLink> refs) {
    return refs.stream()
        .filter(r -> r.type().equals(ActionConfirmationService.PROPOSAL))
        .findFirst()
        .orElseThrow()
        .id();
  }

  @AfterMethod
  public void cleanup() throws Exception {
    SecurityContextHolder.clearContext();
    db.close();
  }

  @Test
  public void storedProposalConfirmsOnceAndReturnsItsCreatedBattle() throws Exception {
    var refs = propose();
    String code = code(refs);
    var stored =
        json.readTree(
            jdbc.queryForObject(
                "SELECT payload FROM assistant_action WHERE code=?", String.class, code));
    assertThat(stored.path("snapshots").path("character:" + receiver).path("bounty").isInt())
        .isTrue();
    var first = actions.handle("confirm " + code, context).orElseThrow();
    var second = actions.handle("confirm " + code, context).orElseThrow();
    assertThat(first).isEqualTo(second);
    assertThat(first.resources()).extracting(ResourceLink::id).containsExactly(battle.toString());
    assertThat(first.resources().get(0).type()).isEqualTo("battle");
    assertThat(actions.proposals(refs, context).get(0).status()).isEqualTo("completed");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM battle_write_probe", Integer.class))
        .isEqualTo(1);
    verify(battles, times(1)).createBattle(any());
  }

  @Test
  public void realChangesStillPreventTheBattleWriteAndReturnLocalizedReason() throws Exception {
    var refs = propose();
    starterView.attack(101);
    var chinese = new ToolContext(user, AssistantLanguage.ZH_CN);
    assertThatThrownBy(() -> actions.handle("确认 " + code(refs), chinese))
        .isInstanceOf(ToolProblem.class)
        .hasMessageContaining("资源信息已更新");
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM battle_write_probe", Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM assistant_action WHERE code=?", String.class, code(refs)))
        .isEqualTo("PENDING");
    verify(battles, never()).createBattle(any());
  }
}
