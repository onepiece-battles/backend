package com.yichi.onepiece.assistant.tools;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.yichi.onepiece.assistant.agent.*;
import com.yichi.onepiece.assistant.dto.AssistantDto.ResourceLink;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import java.util.*;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.*;

public class ResourceToolkitTests {
  private final ObjectMapper json = new ObjectMapper();
  private final ToolContext c = new ToolContext(UUID.randomUUID(), AssistantLanguage.EN_US);
  private CharacterService characters;
  private FruitService fruits;
  private GroupService groups;
  private BattleService battles;
  private IamService iam;
  private ResourceLookup lookup;
  private ResourceQueryTools queries;
  private BattleTools battleTools;

  @BeforeMethod
  public void setup() {
    characters = mock(CharacterService.class);
    fruits = mock(FruitService.class);
    groups = mock(GroupService.class);
    battles = mock(BattleService.class);
    iam = mock(IamService.class);
    lookup = new ResourceLookup(characters, fruits, groups, battles, iam);
    queries = new ResourceQueryTools(lookup);
    battleTools = new BattleTools(lookup);
  }

  private JsonNode args(String raw) throws Exception {
    return json.readTree(raw);
  }

  private I18nText name(String value) {
    return new I18nText().enUS(value).zhCN(value);
  }

  @Test
  public void pagedCharacterFiltersPreserveScopeAndReportRealTotals() throws Exception {
    when(characters.listCharacters(any(), anyBoolean()))
        .thenReturn(
            new ListCharacterResponse()
                .totalSize(61)
                .characters(
                    List.of(new CharacterListView().id(UUID.randomUUID()).name(name("Luffy")))));
    var value =
        json.readTree(
            queries
                .characters(
                    args(
                        "{\"scope\":\"mine\",\"page\":2,\"page_size\":20,\"has_fruit\":false,\"hakis\":[\"Busoshoku\"],\"sort_by\":\"attack\"}"),
                    c)
                .content());
    assertThat(value.path("total_count").asInt()).isEqualTo(61);
    assertThat(value.path("has_more").asBoolean()).isTrue();
    var request = ArgumentCaptor.forClass(ListCharacterRequest.class);
    verify(characters).listCharacters(request.capture(), eq(false));
    assertThat(request.getValue().getCharacterFilter().getUserId()).isEqualTo(c.userId());
    assertThat(request.getValue().getCharacterFilter().getHasFruit()).isFalse();
    assertThat(request.getValue().getCharacterFilter().getHakis()).containsExactly(Haki.BUSOSHOKU);
    assertThat(request.getValue().getPageIndex()).isEqualTo(2);
  }

  @Test
  public void rejectsUnsafeSortsAndPagesBeforeServiceCalls() throws Exception {
    assertThatThrownBy(
            () -> queries.characters(args("{\"sort_by\":\"attack; DROP TABLE fruit\"}"), c))
        .isInstanceOf(ToolProblem.class);
    assertThatThrownBy(() -> queries.fruits(args("{\"page_size\":31}"), c))
        .isInstanceOf(ToolProblem.class);
    assertThatThrownBy(() -> queries.groups(args("{\"page\":-1}"), c))
        .isInstanceOf(ToolProblem.class);
    verifyNoInteractions(characters, fruits, groups);
  }

  @Test
  public void ambiguousNamesRequireUserSelectionButExactMatchWins() {
    UUID a = UUID.randomUUID(), b = UUID.randomUUID();
    when(characters.listCharacters(any()))
        .thenReturn(
            new ListCharacterResponse()
                .characters(
                    List.of(
                        new CharacterListView().id(a).name(name("Luffy")),
                        new CharacterListView().id(b).name(name("Luffy future")))));
    assertThat(lookup.resolve("character", "Luffy", c)).isEqualTo(a);
    assertThatThrownBy(() -> lookup.resolve("character", "Luff", c))
        .isInstanceOf(ToolProblem.class)
        .satisfies(e -> assertThat(((ToolProblem) e).result().resources()).hasSize(2));
    assertThatThrownBy(() -> new CharacterTools(characters, iam).characterDetail("Luff", c))
        .isInstanceOf(ToolProblem.class);
  }

  @Test
  public void membersLinkToCharactersInsteadOfRelationshipRows() throws Exception {
    UUID group = UUID.randomUUID(), character = UUID.randomUUID(), relation = UUID.randomUUID();
    when(groups.getGroupFullView(any()))
        .thenReturn(
            new GroupFullView()
                .id(group)
                .name(name("Crew"))
                .characters(
                    List.of(
                        new CharacterInGroupView()
                            .id(relation)
                            .characterId(character)
                            .name(name("Luffy")))));
    var value = queries.members(args("{\"group\":\"" + group + "\"}"), c);
    assertThat(value.resources())
        .contains(new ResourceLink("character", character.toString(), "Luffy"));
    assertThat(value.content()).doesNotContain(relation.toString());
  }

  @Test
  public void hierarchyIsBoundedAndDetectsCycles() throws Exception {
    UUID root = UUID.randomUUID(), child = UUID.randomUUID();
    when(groups.getGroupFullView(any()))
        .thenReturn(new GroupFullView().id(root).name(name("Root")));
    when(groups.listChildGroups(any()))
        .thenAnswer(
            i -> {
              ListChildGroupsRequest r = i.getArgument(0);
              UUID id = r.getGroupId().equals(root) ? child : root;
              return new ListChildGroupsResponse()
                  .childGroups(List.of(new Group().id(id).name(name("Node"))));
            });
    var value =
        json.readTree(
            queries.hierarchy(args("{\"group\":\"" + root + "\",\"max_depth\":4}"), c).content());
    assertThat(value.path("nodes").size()).isEqualTo(2);
    assertThat(value.path("truncated").asBoolean()).isTrue();
    verify(groups, times(2)).listChildGroups(any());
  }

  @Test
  public void fruitPreviewUsesBaseStatsAndAllCoefficientsWithoutMutatingAnything()
      throws Exception {
    UUID id = UUID.randomUUID(), fruit = UUID.randomUUID();
    when(characters.getCharacterFullView(id))
        .thenReturn(
            new CharacterFullView()
                .id(id)
                .name(name("Luffy"))
                .maximumFruitCount(2)
                .baseAttack(100)
                .baseDefense(100)
                .baseBlood(1000)
                .baseSpeed(100)
                .baseIntelligence(100)
                .baseGroupGain(100)
                .attack(110)
                .fruits(
                    List.of(
                        new FruitListView()
                            .id(UUID.randomUUID())
                            .attackSlope(1.1)
                            .attackIntercept(0)
                            .defenseSlope(1.0)
                            .defenseIntercept(0)
                            .bloodSlope(1.0)
                            .bloodIntercept(0)
                            .speedSlope(1.0)
                            .speedIntercept(0)
                            .intelligenceSlope(1.0)
                            .intelligenceIntercept(0)
                            .groupGainSlope(1.0)
                            .groupGainIntercept(0))));
    when(fruits.getFruit(any()))
        .thenReturn(
            new FruitFullView()
                .id(fruit)
                .name(name("Fruit"))
                .attackSlope(1.5)
                .attackIntercept(10)
                .defenseSlope(1.0)
                .defenseIntercept(0)
                .bloodSlope(1.0)
                .bloodIntercept(0)
                .speedSlope(1.0)
                .speedIntercept(0)
                .intelligenceSlope(1.0)
                .intelligenceIntercept(0)
                .groupGainSlope(1.0)
                .groupGainIntercept(0));
    var value =
        json.readTree(
            queries
                .previewFruit(args("{\"character\":\"" + id + "\",\"fruit\":\"" + fruit + "\"}"), c)
                .content());
    assertThat(value.path("projected_stats").path("attack").asInt()).isEqualTo(175);
    assertThat(value.path("simulation_only").asBoolean()).isTrue();
    verify(characters, never()).eatFruit(any(), any());
    verify(characters, never()).updateCharacter(any());
  }

  @Test
  public void battleSearchUsesBothParticipantFiltersAndStatus() throws Exception {
    when(battles.list(any(), anyBoolean()))
        .thenReturn(new ListBattleResponse().totalSize(0).battles(List.of()));
    battleTools.search(
        args(
            "{\"starter_name\":\"Luffy\",\"receiver_name\":\"Law\",\"statuses\":[\"FINISHED\"],\"scope\":\"system\"}"),
        c);
    var request = ArgumentCaptor.forClass(ListBattleRequest.class);
    verify(battles).list(request.capture(), eq(false));
    assertThat(request.getValue().getBattleFilter().getStarterName()).isEqualTo("Luffy");
    assertThat(request.getValue().getBattleFilter().getReceiverName()).isEqualTo("Law");
    assertThat(request.getValue().getBattleFilter().getSystemResource()).isTrue();
  }

  @Test
  public void headToHeadCoversBothOrientationsAndDoesNotInventWins() throws Exception {
    UUID first = UUID.randomUUID(), second = UUID.randomUUID();
    var x = new com.yichi.onepiece.model.Character().id(first).name(name("Luffy"));
    var y = new com.yichi.onepiece.model.Character().id(second).name(name("Law"));
    when(characters.getCharacterFullView(first))
        .thenReturn(
            new CharacterFullView()
                .id(first)
                .name(name("Luffy"))
                .battles(
                    List.of(
                        new BattleListView()
                            .id(UUID.randomUUID())
                            .starterId(first)
                            .receiverId(second)
                            .starter(x)
                            .receiver(y)
                            .battleExecution(
                                new BattleExecution()
                                    .battleStatus(BattleStatus.FINISHED)
                                    .winnerId(first)),
                        new BattleListView()
                            .id(UUID.randomUUID())
                            .starterId(second)
                            .receiverId(first)
                            .starter(y)
                            .receiver(x)
                            .battleExecution(
                                new BattleExecution().battleStatus(BattleStatus.IN_PROGRESS)),
                        new BattleListView()
                            .id(UUID.randomUUID())
                            .starterId(first)
                            .receiverId(second)
                            .starter(x)
                            .receiver(y)
                            .battleExecution(
                                new BattleExecution().battleStatus(BattleStatus.FAILED)))));
    when(characters.getCharacterFullView(second))
        .thenReturn(new CharacterFullView().id(second).name(name("Law")));
    var value =
        json.readTree(
            battleTools
                .headToHead(args("{\"first\":\"" + first + "\",\"second\":\"" + second + "\"}"), c)
                .content());
    assertThat(value.path("total_count").asInt()).isEqualTo(3);
    assertThat(value.path("first_wins").asInt()).isEqualTo(1);
    assertThat(value.path("second_wins").asInt()).isZero();
    assertThat(value.path("draws").asInt()).isEqualTo(1);
    assertThat(value.path("unresolved").asInt()).isEqualTo(1);
  }

  @Test
  public void schemasRejectModelInjectedOwnershipAndInvalidValues() throws Exception {
    assertThatThrownBy(
            () ->
                ActionSchemas.validate(
                    args(
                        "{\"data\":{\"name\":{\"zh_CN\":\"团体\"},\"group_category\":\"pirate\",\"user_id\":\"victim\"}}"),
                    ActionSchemas.parameters("create_group"),
                    "args"))
        .isInstanceOf(ToolProblem.class);
    assertThatThrownBy(
            () ->
                ActionSchemas.validate(
                    args("{\"character\":\"id\",\"changes\":{\"attack\":2000}}"),
                    ActionSchemas.parameters("update_character"),
                    "args"))
        .isInstanceOf(ToolProblem.class);
    assertThatThrownBy(
            () ->
                ActionSchemas.validate(
                    args("{\"fruit\":\"id\",\"changes\":{\"attack_slope\":\"1.2\"}}"),
                    ActionSchemas.parameters("update_fruit"),
                    "args"))
        .isInstanceOf(ToolProblem.class);
    assertThatThrownBy(
            () ->
                ActionSchemas.validate(
                    args("{\"group\":\"id\",\"changes\":{}}"),
                    ActionSchemas.parameters("update_group"),
                    "args"))
        .isInstanceOf(ToolProblem.class);
  }

  @Test
  public void localizedPartialPatchPreservesOtherLanguageAndEffectiveStats() throws Exception {
    var current =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            args(
                "{\"name\":{\"zh_CN\":\"路飞\",\"en_US\":\"Luffy\"},\"attack\":200,\"base_attack\":100,\"image_uri\":\"oss-avatar\"}");
    ResourceActionExecutor.merge(
        current,
        (com.fasterxml.jackson.databind.node.ObjectNode)
            args("{\"name\":{\"en_US\":\"New Luffy\"},\"attack\":150}"),
        true);
    assertThat(current.path("name").path("zh_CN").asText()).isEqualTo("路飞");
    assertThat(current.path("base_attack").asInt()).isEqualTo(150);
    assertThat(current.path("attack").asInt()).isEqualTo(200);
    assertThat(current.path("image_uri").asText()).isEqualTo("oss-avatar");
  }

  @Test
  public void registeredToolkitHasNoDirectExecutionOrConfirmationTool() {
    var registry =
        new ToolRegistry(
            List.of(
                new CharacterTools(characters, iam),
                new FruitTools(fruits, iam),
                new GroupTools(groups, iam),
                queries,
                battleTools,
                new ResourceActionTools(mock(ActionConfirmationService.class))));
    assertThat(registry.all()).hasSize(42);
    assertThat(registry.all())
        .noneMatch(
            t ->
                Set.of("confirm", "execute", "create_character", "start_battle")
                    .contains(t.name()));
  }
}
