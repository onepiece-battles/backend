package com.yichi.onepiece.assistant.tools;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.yichi.onepiece.assistant.agent.*;
import com.yichi.onepiece.battle.service.*;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.model.UserDetailImpl;
import java.util.*;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testng.annotations.*;

public class ResourceActionExecutorTests {
  private static final UUID USER = UUID.randomUUID(),
      ROLE = UUID.randomUUID(),
      CHARACTER = UUID.randomUUID(),
      FRUIT = UUID.randomUUID(),
      GROUP = UUID.randomUUID();
  private final ObjectMapper json = new ObjectMapper();
  private final ToolContext c = new ToolContext(USER, AssistantLanguage.EN_US);
  private CharacterService characters;
  private FruitService fruits;
  private GroupService groups;
  private IamService iam;
  private ResourceActionExecutor executor;
  private CharacterFullView character;

  @BeforeMethod
  public void setup() {
    characters = mock(CharacterService.class);
    fruits = mock(FruitService.class);
    groups = mock(GroupService.class);
    iam = mock(IamService.class);
    executor =
        new ResourceActionExecutor(
            new ResourceLookup(characters, fruits, groups, mock(BattleService.class), iam),
            mock(BattleExecutionAsyncService.class),
            mock(ActionRecordService.class),
            mock(JdbcTemplate.class));
    var actor = new UserDetailImpl();
    actor.setId(USER);
    actor.setLoginRoleId(ROLE);
    actor.setLoginRole(UserRole.NORMAL);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, List.of()));
    character =
        new CharacterFullView()
            .id(CHARACTER)
            .name(new I18nText().enUS("Luffy").zhCN("路飞"))
            .maximumFruitCount(2)
            .attack(150)
            .baseAttack(100)
            .imageUri("https://oss.example/avatar.png")
            .hakis(List.of(Haki.BUSOSHOKU))
            .creationRecord(new ActionRecord().userId(USER).roleId(ROLE));
    when(characters.getCharacterFullView(CHARACTER)).thenReturn(character);
    when(fruits.getFruit(any()))
        .thenReturn(new FruitFullView().id(FRUIT).name(new I18nText().enUS("Fruit")));
  }

  @AfterMethod
  public void cleanup() {
    SecurityContextHolder.clearContext();
  }

  private JsonNode args(String raw) throws Exception {
    return json.readTree(raw);
  }

  private JsonNode update() throws Exception {
    return args("{\"character\":\"" + CHARACTER + "\",\"changes\":{\"attack\":120}}");
  }

  @Test
  public void battleProposalKeepsStarterAndReceiverRolesExplicit() throws Exception {
    UUID opponent = UUID.randomUUID();
    when(characters.getCharacterFullView(opponent))
        .thenReturn(new CharacterFullView().id(opponent).name(new I18nText().enUS("Law")));
    var plan =
        executor.plan(
            "create_battle",
            args("{\"starter\":\"" + CHARACTER + "\",\"receiver\":\"" + opponent + "\"}"),
            c);
    assertThat(plan.summary()).contains("Starter: [Luffy]", "Receiver: [Law]");
    assertThat(plan.payload().path("input").path("starter").asText())
        .isEqualTo(CHARACTER.toString());
    assertThat(plan.payload().path("input").path("receiver").asText())
        .isEqualTo(opponent.toString());
  }

  @Test
  public void draftRechecksOwnershipAndRejectsChangesFromAnotherRole() throws Exception {
    character.getCreationRecord().roleId(UUID.randomUUID());
    assertThatThrownBy(() -> executor.plan("update_character", update(), c))
        .isInstanceOf(ToolProblem.class)
        .hasMessageContaining("creator");
    verify(characters, never()).updateCharacter(any());
  }

  @Test
  public void nikaCanPrepareAnEditButNothingIsWritten() throws Exception {
    var actor =
        (UserDetailImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    actor.setLoginRole(UserRole.NIKA);
    character.getCreationRecord().userId(UUID.randomUUID());
    var plan = executor.plan("update_character", update(), c);
    assertThat(plan.summary()).contains("100 → 120");
    verify(characters, never()).updateCharacter(any());
  }

  @Test
  public void staleSnapshotsAreRejectedAndPermissionsAreRechecked() throws Exception {
    var plan = executor.plan("update_character", update(), c);
    executor.lockAndCheck("update_character", plan.payload(), c);
    character.baseAttack(101);
    assertThatThrownBy(() -> executor.lockAndCheck("update_character", plan.payload(), c))
        .isInstanceOf(ToolProblem.class)
        .hasMessageContaining("changed");
    doThrow(new com.yichi.onepiece.exception.PermissionDeniedException("revoked"))
        .when(iam)
        .checkPermissionOfCurrentAccount(eq(ResourceType.CHARACTER), eq(Set.of(Permission.UPDATE)));
    assertThatThrownBy(() -> executor.lockAndCheck("update_character", plan.payload(), c))
        .isInstanceOf(com.yichi.onepiece.exception.PermissionDeniedException.class);
    verify(characters, never()).updateCharacter(any());
  }

  @Test
  public void confirmedPatchMergesBaseStatsWithoutClearingOtherFields() throws Exception {
    var plan = executor.plan("update_character", update(), c);
    executor.execute("update_character", plan.payload(), c);
    var request = ArgumentCaptor.forClass(UpdateCharacterRequest.class);
    verify(characters).updateCharacter(request.capture());
    assertThat(request.getValue().getBaseAttack()).isEqualTo(120);
    assertThat(request.getValue().getName().getZhCN()).isEqualTo("路飞");
    assertThat(request.getValue().getHakis()).containsExactly(Haki.BUSOSHOKU);
    assertThat(request.getValue().getImageUri()).isEqualTo(character.getImageUri());
  }

  @Test
  public void duplicateFruitAndCapacityReductionAreRejected() throws Exception {
    character.fruits(List.of(new FruitListView().id(FRUIT)));
    assertThatThrownBy(
            () ->
                executor.plan(
                    "eat_fruit",
                    args("{\"character\":\"" + CHARACTER + "\",\"fruit\":\"" + FRUIT + "\"}"),
                    c))
        .isInstanceOf(ToolProblem.class)
        .hasMessageContaining("already");
    character.fruits(
        List.of(new FruitListView().id(FRUIT), new FruitListView().id(UUID.randomUUID())));
    assertThatThrownBy(
            () ->
                executor.plan(
                    "update_character",
                    args(
                        "{\"character\":\""
                            + CHARACTER
                            + "\",\"changes\":{\"maximum_fruit_count\":1}}"),
                    c))
        .isInstanceOf(ToolProblem.class)
        .hasMessageContaining("capacity");
  }

  @Test
  public void groupContainerCannotAcceptCharactersAndCyclesAreRejected() throws Exception {
    UUID parent = UUID.randomUUID();
    var view =
        new GroupFullView()
            .id(GROUP)
            .name(new I18nText().enUS("Crew"))
            .groupCategory(GroupCategory.GROUP)
            .creationRecord(new ActionRecord().userId(USER).roleId(ROLE))
            .fatherGroups(List.of(new Group().id(parent)));
    when(groups.getGroupFullView(any()))
        .thenAnswer(
            i -> {
              GetGroupRequest request = i.getArgument(0);
              return request.getId().equals(GROUP)
                  ? view
                  : new GroupFullView()
                      .id(parent)
                      .name(new I18nText().enUS("Parent"))
                      .groupCategory(GroupCategory.GROUP)
                      .creationRecord(new ActionRecord().userId(USER).roleId(ROLE));
            });
    assertThatThrownBy(
            () ->
                executor.plan(
                    "join_group",
                    args(
                        "{\"character\":\""
                            + CHARACTER
                            + "\",\"group\":\""
                            + GROUP
                            + "\",\"position\":\"member\"}"),
                    c))
        .isInstanceOf(ToolProblem.class);
    assertThatThrownBy(
            () ->
                executor.plan(
                    "attach_subgroups",
                    args("{\"group\":\"" + GROUP + "\",\"children\":[\"" + parent + "\"]}"),
                    c))
        .isInstanceOf(ToolProblem.class)
        .hasMessageContaining("cycle");
    verify(groups, never()).addGroup(any());
  }
}
