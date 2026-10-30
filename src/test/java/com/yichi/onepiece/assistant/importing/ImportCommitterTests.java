package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.AssistantLanguage;
import com.yichi.onepiece.assistant.importing.ImportCommitter.ImportRejected;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import java.util.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ImportCommitterTests {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final UUID userId = UUID.randomUUID(), roleId = UUID.randomUUID();
  private CharacterService characters;
  private FruitService fruits;
  private GroupService groups;
  private ExistingResources existing;
  private ImportCommitter committer;

  @BeforeMethod
  public void setup() {
    characters = mock(CharacterService.class);
    fruits = mock(FruitService.class);
    groups = mock(GroupService.class);
    existing = mock(ExistingResources.class);
    when(existing.find(any(), any(), any())).thenReturn(Optional.empty());
    committer = new ImportCommitter(characters, fruits, groups, mock(IamService.class), existing);
  }

  static ObjectNode name(String zh, String en) {
    return JSON.createObjectNode().put("zh_CN", zh).put("en_US", en);
  }

  static ObjectNode character(String zh, String en, int fruitCapacity) {
    var data = JSON.createObjectNode();
    data.set("name", name(zh, en));
    data.put("race", "HUMAN").put("battle_style", "BODY").put("maximum_fruit_count", fruitCapacity);
    data.put("group_gain", 120).put("attack", 900).put("defense", 600).put("blood", 5000);
    data.put("speed", 120).put("intelligence", 150).put("energy", 150).put("bounty", 3000000000L);
    data.putArray("hakis").add("Haoshoku");
    return data;
  }

  static ObjectNode fruit(String zh, String en) {
    var data = JSON.createObjectNode();
    data.set("name", name(zh, en));
    data.put("fruit_type", "Mythical_Zoan").put("energy_consume", 5);
    for (String stat : List.of("attack", "defense", "blood", "group_gain", "speed", "intelligence"))
      data.put(stat + "_slope", 1.2).put(stat + "_intercept", 10);
    return data;
  }

  static ObjectNode group(String zh, String en, String category) {
    var data = JSON.createObjectNode();
    data.set("name", name(zh, en));
    return data.put("group_category", category);
  }

  static ImportItem item(String key, String type, ObjectNode data) {
    return new ImportItem(key, type, data, null, List.of(), null, null, List.of(), List.of());
  }

  static ImportItem member(String key, ObjectNode data, List<String> fruits, String group, String position) {
    return new ImportItem(key, CHARACTER, data, null, fruits, group, position, List.of(), List.of());
  }

  static ImportItem reuse(String key, String type, UUID id, ObjectNode name) {
    var data = JSON.createObjectNode();
    data.set("name", name);
    return new ImportItem(key, type, data, id.toString(), List.of(), null, null, List.of(), List.of());
  }

  @Test
  public void importsGroupsFruitsAndCharactersThenTheirRelationsInOneBatch() {
    UUID groupId = UUID.randomUUID(), newFruitId = UUID.randomUUID(), luffyId = UUID.randomUUID();
    UUID gomuId = UUID.randomUUID(), zoroId = UUID.randomUUID();
    when(groups.create(any())).thenReturn(new CreateGroupResponse().id(groupId));
    when(fruits.createFruit(any())).thenReturn(new Fruit().id(newFruitId));
    when(characters.createCharacter(any())).thenReturn(new com.yichi.onepiece.model.Character().id(luffyId));
    when(fruits.getFruit(any())).thenReturn(new FruitFullView().id(gomuId));
    when(characters.getCharacterFullView(zoroId))
        .thenReturn(new CharacterFullView().id(zoroId).maximumFruitCount(1).fruits(List.of()).groups(List.of()));

    var result =
        committer.commit(
            List.of(
                item("g1", GROUP, group("草帽一伙", "Straw Hat Pirates", "pirate")),
                reuse("f1", FRUIT, gomuId, name("橡胶果实", "Gomu Gomu no Mi")),
                item("f2", FRUIT, fruit("人人果实 幻兽种 模型尼卡", "Hito Hito no Mi, Model: Nika")),
                member("c1", character("蒙奇·D·路飞", "Monkey D. Luffy", 2), List.of("f1", "f2"), "g1", "captain"),
                new ImportItem("c2", CHARACTER, JSON.createObjectNode().set("name", name("罗罗诺亚·索隆", "Roronoa Zoro")),
                    zoroId.toString(), List.of(), "g1", "combatant", List.of(), List.of())),
            userId,
            roleId,
            AssistantLanguage.ZH_CN);

    InOrder order = inOrder(groups, fruits, characters);
    order.verify(groups).create(any());
    order.verify(fruits).createFruit(any());
    var created = ArgumentCaptor.forClass(CreateCharacterRequest.class);
    order.verify(characters).createCharacter(created.capture());
    order.verify(characters).eatFruit(luffyId, gomuId);
    order.verify(characters).eatFruit(luffyId, newFruitId);
    var joins = ArgumentCaptor.forClass(JoinGroupRequest.class);
    order.verify(characters, times(2)).joinGroup(joins.capture());
    assertThat(created.getValue().getName().getZhCN()).isEqualTo("蒙奇·D·路飞");
    assertThat(created.getValue().getName().getEnUS()).isEqualTo("Monkey D. Luffy");
    var group = ArgumentCaptor.forClass(CreateGroupRequest.class);
    verify(groups).create(group.capture());
    assertThat(group.getValue().getName().getEnUS()).isEqualTo("Straw Hat Pirates");
    assertThat(group.getValue().getGroupCategory()).isEqualTo(GroupCategory.PIRATE);
    var createdFruit = ArgumentCaptor.forClass(CreateFruitRequest.class);
    verify(fruits).createFruit(createdFruit.capture());
    assertThat(createdFruit.getValue().getName().getZhCN()).isEqualTo("人人果实 幻兽种 模型尼卡");
    assertThat(createdFruit.getValue().getAttackSlope()).isEqualTo(1.2);
    assertThat(created.getValue().getAttack()).isEqualTo(900);
    assertThat(created.getValue().getBounty()).isEqualTo(3000000000L);
    assertThat(created.getValue().getUserId()).isEqualTo(userId);
    assertThat(created.getValue().getRoleId()).isEqualTo(roleId);
    assertThat(joins.getAllValues())
        .extracting(JoinGroupRequest::getCharacterId, JoinGroupRequest::getGroupId, JoinGroupRequest::getPosition)
        .containsExactly(
            tuple(luffyId, groupId, GroupPosition.CAPTAIN), tuple(zoroId, groupId, GroupPosition.COMBATANT));
    assertThat(result.created()).extracting(CreatedResource::key).containsExactly("g1", "f2", "c1");
    assertThat(result.created().get(2).name()).isEqualTo("蒙奇·D·路飞");
    assertThat(result.reused()).isEqualTo(2);
    assertThat(result.fruitsEaten()).isEqualTo(2);
    assertThat(result.groupsJoined()).isEqualTo(2);
  }

  @Test
  public void reportsEveryProblemAndWritesNothing() {
    var tooStrong = character("凯多", "Kaidou", 1).put("attack", 99999);
    when(existing.find(eq(FRUIT), any(), any())).thenReturn(Optional.of(UUID.randomUUID()));

    var rejected =
        catchThrowableOfType(
            () ->
                committer.commit(
                    List.of(
                        item("c1", CHARACTER, tooStrong),
                        member("c2", character("马尔高", "Marco", 1), List.of("f9"), null, null),
                        item("f1", FRUIT, fruit("鱼鱼果实", "Uo Uo no Mi")),
                        item("g1", GROUP, group("四皇", "Four Emperors", "group")),
                        item("g2", GROUP, group("四皇 ", "four emperors", "pirate")),
                        member("c3", character("杰克", "Jack", 1), List.of(), "g1", "member")),
                    userId,
                    roleId,
                    AssistantLanguage.ZH_CN),
            ImportRejected.class);

    // f1 both exists already and is eaten by no one.
    assertThat(rejected.problems())
        .extracting(Problem::key)
        .containsExactlyInAnyOrder("c1", "c2", "f1", "f1", "g2", "c3");
    assertThat(rejected.problems()).filteredOn(p -> p.key().equals("c1")).first()
        .extracting(Problem::message).asString().contains("attack");
    verify(groups, never()).create(any());
    verify(fruits, never()).createFruit(any());
    verify(characters, never()).createCharacter(any());
    verify(characters, never()).eatFruit(any(), any());
    verify(characters, never()).joinGroup(any());
  }

  @Test
  public void anExistingCharacterKeepsTheFruitsAndGroupItAlreadyHas() {
    UUID luffyId = UUID.randomUUID(), gomuId = UUID.randomUUID(), crewId = UUID.randomUUID();
    when(fruits.getFruit(any())).thenReturn(new FruitFullView().id(gomuId));
    when(groups.getGroupFullView(any()))
        .thenReturn(new GroupFullView().id(crewId).groupCategory(GroupCategory.PIRATE));
    when(characters.getCharacterFullView(luffyId))
        .thenReturn(
            new CharacterFullView()
                .id(luffyId)
                .maximumFruitCount(1)
                .fruits(List.of(new FruitListView().id(gomuId)))
                .groups(List.of(new GroupForCharacterView().groupId(crewId).onceBelongedTo(false))));

    var result =
        committer.commit(
            List.of(
                reuse("g1", GROUP, crewId, name("草帽一伙", "Straw Hat Pirates")),
                reuse("f1", FRUIT, gomuId, name("橡胶果实", "Gomu Gomu no Mi")),
                new ImportItem("c1", CHARACTER, JSON.createObjectNode().set("name", name("路飞", "Luffy")),
                    luffyId.toString(), List.of("f1"), "g1", "captain", List.of(), List.of())),
            userId,
            roleId,
            AssistantLanguage.EN_US);

    verify(characters, never()).eatFruit(any(), any());
    verify(characters, never()).joinGroup(any());
    assertThat(result.created()).isEmpty();
    assertThat(result.reused()).isEqualTo(3);
  }

  @Test
  public void aFullExistingCharacterCannotEatAnotherFruit() {
    UUID kaidoId = UUID.randomUUID();
    when(characters.getCharacterFullView(kaidoId))
        .thenReturn(
            new CharacterFullView()
                .id(kaidoId)
                .maximumFruitCount(1)
                .fruits(List.of(new FruitListView().id(UUID.randomUUID())))
                .groups(List.of()));

    var rejected =
        catchThrowableOfType(
            () ->
                committer.commit(
                    List.of(
                        item("f1", FRUIT, fruit("鱼鱼果实", "Uo Uo no Mi")),
                        new ImportItem("c1", CHARACTER, JSON.createObjectNode().set("name", name("凯多", "Kaido")),
                            kaidoId.toString(), List.of("f1"), null, null, List.of(), List.of())),
                    userId,
                    roleId,
                    AssistantLanguage.EN_US),
            ImportRejected.class);

    assertThat(rejected.problems()).extracting(Problem::key).containsExactly("c1");
    verify(fruits, never()).createFruit(any());
  }

  @Test
  public void aRemovedExistingResourceIsReportedInsteadOfFailingTheRequest() {
    UUID gone = UUID.randomUUID();
    when(groups.getGroupFullView(any())).thenThrow(new NoSuchElementException());

    var rejected =
        catchThrowableOfType(
            () ->
                committer.commit(
                    List.of(reuse("g1", GROUP, gone, name("百兽海贼团", "Beasts Pirates"))),
                    userId,
                    roleId,
                    AssistantLanguage.ZH_CN),
            ImportRejected.class);

    assertThat(rejected.problems()).extracting(Problem::key).containsExactly("g1");
  }

  @Test
  public void aNewFruitNobodyEatsIsNotImported() {
    UUID kage = UUID.randomUUID();
    when(fruits.getFruit(any())).thenReturn(new FruitFullView().id(kage));

    var rejected =
        catchThrowableOfType(
            () ->
                committer.commit(
                    List.of(
                        item("f1", FRUIT, fruit("野猪果实", "Boar Boar Fruit")),
                        reuse("f2", FRUIT, kage, name("影子果实", "Kage Kage no Mi")),
                        member("c1", character("佩罗娜", "Perona", 1), List.of(), null, null)),
                    userId,
                    roleId,
                    AssistantLanguage.ZH_CN),
            ImportRejected.class);

    // The reused fruit needs no holder: nothing is created for it.
    assertThat(rejected.problems()).extracting(Problem::key).containsExactly("f1");
    assertThat(rejected.problems().get(0).message()).contains("持有者");
    verify(fruits, never()).createFruit(any());
  }
}
