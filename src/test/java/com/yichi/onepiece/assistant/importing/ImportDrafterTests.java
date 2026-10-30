package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.assistant.agent.AssistantException;
import com.yichi.onepiece.assistant.agent.AssistantLanguage;
import com.yichi.onepiece.assistant.agent.ChatCompletionClient;
import com.yichi.onepiece.assistant.service.AiSettingService;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ImportDrafterTests {
  private static final ObjectMapper JSON = new ObjectMapper();
  private final UUID userId = UUID.randomUUID();
  private AiSettingService settings;
  private ExistingResources existing;
  private FakeDeepSeek deepSeek;
  private ImportDrafter drafter;
  private final List<String> progress = new ArrayList<>();

  /** Answers the roster prompt with {@link #roster} and each detail call with {@link #details}. */
  static final class FakeDeepSeek implements ChatCompletionClient {
    String roster;
    Function<JsonNode, String> details = request -> "{\"items\":[]}";
    final List<CompletionRequest> requests = Collections.synchronizedList(new ArrayList<>());
    final Deque<String> scripted = new ArrayDeque<>();

    @Override
    public Completion complete(CompletionRequest request, Consumer<String> onContentDelta) {
      requests.add(request);
      String content;
      if (!scripted.isEmpty()) content = scripted.poll();
      else if (request.messages().get(0).path("content").asText().equals(ImportPrompts.ROSTER))
        content = roster;
      else {
        try {
          content = details.apply(JSON.readTree(request.messages().get(1).path("content").asText()));
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      }
      return new Completion(content, List.of(), Usage.NONE);
    }
  }

  @BeforeMethod
  public void setup() {
    settings = mock(AiSettingService.class);
    when(settings.isConfigured()).thenReturn(true);
    when(settings.baseUrl()).thenReturn("https://api.deepseek.test/chat/completions");
    when(settings.apiKey()).thenReturn("sk-test");
    when(settings.model()).thenReturn("deepseek-chat");
    existing = mock(ExistingResources.class);
    when(existing.find(any(), any(), any())).thenReturn(Optional.empty());
    when(existing.referenceCharacters(any(), anyInt()))
        .thenReturn(List.of(JSON.createObjectNode().put("name", "路飞").put("attack", 900)));
    when(existing.referenceFruits(any(), anyInt())).thenReturn(List.of());
    deepSeek = new FakeDeepSeek();
    drafter = new ImportDrafter(settings, deepSeek, existing, Runnable::run);
    progress.clear();
  }

  private static String character(String key, String extra) {
    return "{\"key\":\"" + key + "\",\"data\":{\"race\":\"HUMAN\",\"battle_style\":\"KATANA\","
        + "\"maximum_fruit_count\":1,\"group_gain\":120,\"attack\":800,\"defense\":500,"
        + "\"blood\":4000,\"speed\":110,\"intelligence\":120,\"energy\":35" + extra + "}}";
  }

  private static String fruit(String key) {
    var data = JSON.createObjectNode().put("fruit_type", "Paramecia").put("energy_consume", 4);
    for (String stat : List.of("attack", "defense", "blood", "group_gain", "speed", "intelligence"))
      data.put(stat + "_slope", 1.1).put(stat + "_intercept", 5);
    return "{\"key\":\"" + key + "\",\"data\":" + data + "}";
  }

  private ImportItem item(ImportDraft draft, String key) {
    return draft.items().stream().filter(i -> i.key().equals(key)).findFirst().orElseThrow();
  }

  @Test
  public void draftsACrewWithServerKeysRelationsAndExistingResourcesReused() {
    UUID gomu = UUID.randomUUID();
    when(existing.find(eq(FRUIT), argThat(n -> "橡胶果实".equals(n.getZhCN())), any()))
        .thenReturn(Optional.of(gomu));
    deepSeek.roster =
        """
        {"groups":[{"key":"crew","name":{"zh_CN":"草帽一伙","en_US":"Straw Hat Pirates"},"group_category":"pirate"}],
         "fruits":[{"key":"gomu","name":{"zh_CN":"橡胶果实","en_US":"Gomu Gomu no Mi"},"fruit_type":"Paramecia"},
                   {"key":"hana","name":{"zh_CN":"花花果实","en_US":"Hana Hana no Mi"},"fruit_type":"Paramecia"}],
         "characters":[{"key":"luffy","name":{"zh_CN":"蒙奇·D·路飞","en_US":"Monkey D. Luffy"},"fruits":["gomu"],"group":"crew","position":"captain"},
                       {"key":"zoro","name":{"zh_CN":"罗罗诺亚·索隆","en_US":"Roronoa Zoro"},"fruits":["nope"],"group":"crew","position":"swordsman"},
                       {"key":"robin","name":{"zh_CN":"妮可·罗宾","en_US":"Nico Robin"},"fruits":["hana"],"group":"crew","position":"scholar"}],
         "notes":["只包含当前成员"]}
        """;
    var asked = new ArrayList<String>();
    deepSeek.details =
        request -> {
          request.forEach(r -> asked.add(r.path("key").asText()));
          return "{\"items\":[" + character("c1", "") + "," + character("c2", "") + ","
              + character("c3", "") + "," + fruit("f2") + "]}";
        };

    var draft = drafter.draft("草帽一伙", userId, AssistantLanguage.ZH_CN, progress::add);

    assertThat(draft.items()).extracting(ImportItem::key).containsExactly("g1", "f1", "f2", "c1", "c2", "c3");
    assertThat(item(draft, "f1").existingId()).isEqualTo(gomu.toString());
    assertThat(asked).containsExactlyInAnyOrder("f2", "c1", "c2", "c3"); // not the reused fruit
    var luffy = item(draft, "c1");
    assertThat(luffy.fruits()).containsExactly("f1");
    assertThat(luffy.group()).isEqualTo("g1");
    assertThat(luffy.position()).isEqualTo("captain");
    assertThat(luffy.problems()).isEmpty();
    assertThat(luffy.data().path("name").path("en_US").asText()).isEqualTo("Monkey D. Luffy");
    assertThat(luffy.data().path("hakis").isArray()).isTrue();
    var zoro = item(draft, "c2");
    assertThat(zoro.fruits()).isEmpty(); // unknown fruit key dropped, with a note
    assertThat(zoro.position()).isEqualTo("member"); // unknown position falls back
    assertThat(item(draft, "g1").data().path("group_category").asText()).isEqualTo("pirate");
    assertThat(item(draft, "f2").problems()).isEmpty();
    assertThat(draft.notes()).hasSize(2).first().isEqualTo("只包含当前成员");
    assertThat(draft.notes().get(1)).contains("罗罗诺亚·索隆", "「nope」");
    assertThat(progress).isNotEmpty();
    var detailRequest = deepSeek.requests.get(1);
    assertThat(detailRequest.jsonOutput()).isTrue();
    assertThat(detailRequest.tools()).isNull();
    assertThat(detailRequest.messages().get(0).path("content").asText())
        .contains("attack: integer 10–1500")
        .contains("\"name\":\"路飞\"");
  }

  @Test
  public void notesFruitsNobodyEatsAndRelationsItCouldNotLink() {
    when(existing.find(
            eq(FRUIT), argThat(n -> Set.of("影子果实", "透明果实").contains(n.getZhCN())), any()))
        .thenReturn(Optional.of(UUID.randomUUID()));
    // The model names Perona's fruit and crew instead of using their keys, and adds a fruit
    // that no one in the crew eats. An unheld fruit that already exists is only reused: no note.
    deepSeek.roster =
        """
        {"groups":[{"key":"crew","name":{"zh_CN":"月光莫利亚海贼团"},"group_category":"pirate"}],
         "fruits":[{"key":"kage","name":{"zh_CN":"影子果实"},"fruit_type":"Paramecia"},
                   {"key":"suke","name":{"zh_CN":"透明果实"},"fruit_type":"Paramecia"},
                   {"key":"horo","name":{"zh_CN":"灵灵果实"},"fruit_type":"Paramecia"},
                   {"key":"boar","name":{"zh_CN":"野猪果实"},"fruit_type":"Zoan"}],
         "characters":[{"key":"moria","name":{"zh_CN":"杰克斯·莫利亚"},"fruits":["kage"],"group":"crew"},
                       {"key":"perona","name":{"zh_CN":"佩罗娜"},"fruits":["灵灵果实"],"group":"月光莫利亚海贼团"},
                       {"key":"absalom","name":{"zh_CN":"阿布萨罗姆"},"group":"crew"}]}
        """;

    var draft = drafter.draft("月光莫利亚海贼团", userId, AssistantLanguage.ZH_CN, progress::add);

    assertThat(item(draft, "c2").fruits()).isEmpty();
    assertThat(item(draft, "c2").group()).isNull();
    assertThat(draft.notes())
        .containsExactly(
            "「佩罗娜」的果实「灵灵果实」不在名单里，已忽略，请确认。",
            "「佩罗娜」的团体「月光莫利亚海贼团」不在名单里，已忽略，请确认。",
            "「灵灵果实」：草稿里没有角色吃这个果实，请确认是否需要导入。",
            "「野猪果实」：草稿里没有角色吃这个果实，请确认是否需要导入。");
    assertThat(item(draft, "c1").fruits()).containsExactly("f1");
  }

  @Test
  public void repairsWhatItCanAndReportsWhatTheReviewerMustFix() {
    deepSeek.roster =
        """
        {"characters":[{"key":"a","name":{"zh_CN":"凯多","en_US":"Kaido"}},
                       {"key":"b","name":{"zh_CN":"大妈","en_US":"Big Mom"}}]}
        """;
    deepSeek.details =
        request ->
            "{\"items\":["
                + character("c1", ",\"attack\":99999,\"bounty\":\"4,611,100,000\",\"hakis\":[\"Haoshoku\",\"Ryuo\"],\"favourite_food\":\"酒\"")
                + ",{\"key\":\"c2\",\"data\":{\"race\":\"TITAN\",\"attack\":1000},\"uncertain\":[\"attack\"]}]}";

    var draft = drafter.draft("四皇", userId, AssistantLanguage.ZH_CN, progress::add);

    var kaido = item(draft, "c1");
    assertThat(kaido.data().path("attack").asInt()).isEqualTo(1500);
    assertThat(kaido.data().path("bounty").asLong()).isEqualTo(4611100000L);
    assertThat(kaido.data().path("hakis")).containsExactly(JSON.getNodeFactory().textNode("Haoshoku"));
    assertThat(kaido.data().has("favourite_food")).isFalse();
    assertThat(kaido.uncertain()).contains("attack", "bounty", "hakis");
    assertThat(kaido.problems()).isEmpty();
    var bigMom = item(draft, "c2");
    assertThat(bigMom.data().has("race")).isFalse();
    assertThat(bigMom.uncertain()).contains("race", "attack");
    assertThat(bigMom.problems()).singleElement().asString().contains("race").contains("defense");
  }

  @Test
  public void aCharacterWithTwoFruitsGetsCapacityTwoAndThreeFruitsIsAProblem() {
    deepSeek.roster =
        """
        {"fruits":[{"key":"a","name":{"zh_CN":"暗暗果实"},"fruit_type":"Logia"},
                   {"key":"b","name":{"zh_CN":"震震果实"},"fruit_type":"Paramecia"},
                   {"key":"c","name":{"zh_CN":"虚构果实"},"fruit_type":"Paramecia"}],
         "characters":[{"key":"x","name":{"zh_CN":"马歇尔·D·蒂奇"},"fruits":["a","b"]},
                       {"key":"y","name":{"zh_CN":"测试角色"},"fruits":["a","b","c"]}]}
        """;
    deepSeek.details =
        request -> {
          var items = new ArrayList<String>();
          request.forEach(
              r -> items.add(
                  r.path("type").asText().equals(FRUIT) ? fruit(r.path("key").asText()) : character(r.path("key").asText(), "")));
          return "{\"items\":[" + String.join(",", items) + "]}";
        };

    var draft = drafter.draft("黑胡子", userId, AssistantLanguage.ZH_CN, progress::add);

    var teach = item(draft, "c1");
    assertThat(teach.data().path("maximum_fruit_count").asInt()).isEqualTo(2);
    assertThat(teach.uncertain()).contains("maximum_fruit_count");
    assertThat(teach.problems()).isEmpty();
    assertThat(item(draft, "c2").problems()).isNotEmpty();
    assertThat(item(draft, "f1").data().path("fruit_type").asText()).isEqualTo("Logia");
  }

  @Test
  public void aReplyThatIsNotJsonIsRetriedOnceThenReported() {
    deepSeek.roster = "{\"groups\":[{\"key\":\"g\",\"name\":{\"zh_CN\":\"海军\"},\"group_category\":\"marine\"}]}";
    deepSeek.scripted.add("Sure! Here is the roster:");
    var draft = drafter.draft("海军", userId, AssistantLanguage.ZH_CN, progress::add);
    assertThat(draft.items()).extracting(ImportItem::key).containsExactly("g1");

    deepSeek.scripted.add("not json");
    deepSeek.scripted.add("still not json");
    assertThatThrownBy(() -> drafter.draft("海军", userId, AssistantLanguage.ZH_CN, progress::add))
        .isInstanceOf(AssistantException.class)
        .extracting(e -> ((AssistantException) e).code())
        .isEqualTo(AssistantException.UPSTREAM_ERROR);
  }

  @Test
  public void largeScopesAreCutWithANoteAndEmptyOnesRejected() {
    var characters = new StringJoiner(",");
    for (int i = 0; i < 35; i++)
      characters.add("{\"key\":\"k" + i + "\",\"name\":{\"en_US\":\"Marine " + i + "\"}}");
    deepSeek.roster = "{\"characters\":[" + characters + "]}";
    var draft = drafter.draft("all marines", userId, AssistantLanguage.EN_US, progress::add);
    assertThat(draft.items()).hasSize(ImportDrafter.MAX_CHARACTERS);
    assertThat(draft.notes()).singleElement().asString().contains("30");

    deepSeek.roster = "{\"characters\":[]}";
    assertThatThrownBy(() -> drafter.draft("nobody", userId, AssistantLanguage.EN_US, progress::add))
        .isInstanceOf(AssistantException.class)
        .extracting(e -> ((AssistantException) e).code())
        .isEqualTo(AssistantException.INVALID_REQUEST);
  }

  @Test
  public void withoutAKeyNothingIsAsked() {
    when(settings.isConfigured()).thenReturn(false);
    assertThatThrownBy(() -> drafter.draft("草帽一伙", userId, AssistantLanguage.ZH_CN, progress::add))
        .isInstanceOf(AssistantException.class)
        .extracting(e -> ((AssistantException) e).code())
        .isEqualTo(AssistantException.NOT_CONFIGURED);
    assertThat(deepSeek.requests).isEmpty();
  }

  /** A draft item's data: a detail answer's fields plus a name. */
  private static ObjectNode data(String detail, String zh, String en) throws Exception {
    var data = (ObjectNode) JSON.readTree(detail).get("data");
    data.set("name", JSON.createObjectNode().put("zh_CN", zh).put("en_US", en));
    return data;
  }

  private static ImportItem drafted(
      String key, String type, ObjectNode data, List<String> fruits, String group, String position) {
    return new ImportItem(key, type, data, null, fruits, group, position, List.of(), List.of());
  }

  private static ImportItem crew() {
    var data = JSON.createObjectNode().put("group_category", "pirate");
    data.set("name", JSON.createObjectNode().put("zh_CN", "月光·莫利亚海贼团").put("en_US", "Thriller Bark Pirates"));
    return drafted("g1", GROUP, data, List.of(), null, null);
  }

  @Test
  public void revisesOnlyWhatTheReviewerAsksAndKeepsTheirEdits() throws Exception {
    // The reviewer already set Moria's bounty by hand.
    var current =
        List.of(
            crew(),
            drafted("f1", FRUIT, data(fruit("f1"), "影子果实", "Kage Kage no Mi"), List.of(), null, null),
            drafted(
                "c1", ImportDto.CHARACTER, data(character("c1", ",\"bounty\":320000000"), "月光·莫利亚", "Gecko Moria"),
                List.of("f1"), "g1", "captain"),
            drafted("c2", ImportDto.CHARACTER, data(character("c2", ""), "霍古巴克", "Hogback"), List.of(), "g1", "member"));
    deepSeek.scripted.add(
        """
        {"reply":"去掉了霍古巴克，加上了佩罗娜和她的灵灵果实。",
         "remove":["c2"],
         "update":[{"key":"c1","name":{"en_US":"Gecko Moria (Warlord)"},"data":{"speed":999}}],
         "add":{"fruits":[{"key":"horo","name":{"zh_CN":"灵灵果实","en_US":"Horo Horo no Mi"},"fruit_type":"Paramecia"}],
                "characters":[{"key":"perona","name":{"zh_CN":"佩罗娜","en_US":"Perona"},"fruits":["horo"],
                               "group":"g1","position":"combatant"}]},
         "notes":["佩罗娜是幽灵公主"]}
        """);
    var asked = new ArrayList<String>();
    deepSeek.details =
        request -> {
          request.forEach(r -> asked.add(r.path("key").asText()));
          return "{\"items\":[" + fruit("f2") + "," + character("c3", "") + "]}";
        };
    var history =
        List.of(new Turn("user", "月光·莫利亚海贼团"), new Turn("assistant", "起草了 1 个团体、1 个果实、2 个角色。"));

    var revision =
        drafter.revise(current, history, "去掉霍古巴克，加上佩罗娜", userId, AssistantLanguage.ZH_CN, progress::add);

    assertThat(revision.reply()).isEqualTo("去掉了霍古巴克，加上了佩罗娜和她的灵灵果实。");
    // A removed key is never handed out again: Perona is c3, not c2.
    assertThat(revision.items()).extracting(ImportItem::key).containsExactly("g1", "f1", "c1", "f2", "c3");
    assertThat(revision.changed()).containsExactly("c1", "f2", "c3");
    assertThat(asked).containsExactlyInAnyOrder("f2", "c3");
    var moria = item(new ImportDraft(revision.items(), revision.notes()), "c1");
    assertThat(moria.data().path("bounty").asLong()).isEqualTo(320000000L);
    assertThat(moria.data().path("speed").asInt()).isEqualTo(200); // clamped to the range
    assertThat(moria.uncertain()).contains("speed");
    assertThat(moria.data().path("name").path("zh_CN").asText()).isEqualTo("月光·莫利亚");
    assertThat(moria.data().path("name").path("en_US").asText()).isEqualTo("Gecko Moria (Warlord)");
    assertThat(moria.fruits()).containsExactly("f1");
    var perona = item(new ImportDraft(revision.items(), revision.notes()), "c3");
    assertThat(perona.fruits()).containsExactly("f2");
    assertThat(perona.group()).isEqualTo("g1");
    assertThat(perona.position()).isEqualTo("combatant");
    assertThat(perona.problems()).isEmpty();
    assertThat(revision.notes()).containsExactly("佩罗娜是幽灵公主"); // every fruit has a holder

    var messages = deepSeek.requests.get(0).messages();
    assertThat(messages).extracting(m -> m.path("role").asText())
        .containsExactly("system", "user", "assistant", "user");
    assertThat(messages.get(3).path("content").asText())
        .contains("Request: 去掉霍古巴克，加上佩罗娜", "\"key\":\"c2\"", "\"existing\":false", "320000000");
  }

  @Test
  public void notesFruitsLeftWithoutHolderAndLinksItCannotMake() throws Exception {
    var current =
        List.of(
            crew(),
            drafted("f1", FRUIT, data(fruit("f1"), "影子果实", "Kage Kage no Mi"), List.of(), null, null),
            drafted(
                "c1", ImportDto.CHARACTER, data(character("c1", ""), "月光·莫利亚", "Gecko Moria"), List.of("f1"), "g1",
                "captain"));
    // The model drops Moria but keeps his fruit, names a fruit instead of its key and points at the
    // crew it just removed.
    deepSeek.scripted.add(
        """
        {"reply":"好的。","remove":["c1","g1"],"update":[{"key":"c9","data":{"attack":100}}],
         "add":{"characters":[{"key":"absalom","name":{"zh_CN":"阿布萨罗姆","en_US":"Absalom"},
                               "fruits":["透明果实"],"group":"g1"}]}}
        """);
    deepSeek.details = request -> "{\"items\":[" + character("c2", "") + "]}";

    var revision = drafter.revise(current, List.of(), "换成阿布萨罗姆", userId, AssistantLanguage.ZH_CN, progress::add);

    assertThat(revision.items()).extracting(ImportItem::key).containsExactly("f1", "c2");
    var absalom = revision.items().get(1);
    assertThat(absalom.fruits()).isEmpty();
    assertThat(absalom.group()).isNull();
    assertThat(revision.notes())
        .containsExactly(
            "「阿布萨罗姆」的果实「透明果实」不在名单里，已忽略，请确认。",
            "「阿布萨罗姆」的团体「g1」不在名单里，已忽略，请确认。",
            "「影子果实」：草稿里没有角色吃这个果实，请确认是否需要导入。");
  }

  @Test
  public void anExistingItemOnlyChangesItsRelations() throws Exception {
    var name = JSON.createObjectNode();
    name.set("name", JSON.createObjectNode().put("zh_CN", "佩罗娜").put("en_US", "Perona"));
    var current =
        List.of(
            drafted("f1", FRUIT, data(fruit("f1"), "灵灵果实", "Horo Horo no Mi"), List.of(), null, null),
            new ImportItem("c1", ImportDto.CHARACTER, name, UUID.randomUUID().toString(), List.of(), null, null,
                List.of(), List.of()));
    deepSeek.scripted.add(
        "{\"reply\":\"好的。\",\"update\":[{\"key\":\"c1\",\"data\":{\"attack\":100},\"fruits\":[\"f1\"]}]}");

    var revision = drafter.revise(current, null, "灵灵果实给佩罗娜", userId, AssistantLanguage.ZH_CN, progress::add);

    var perona = revision.items().get(1);
    assertThat(perona.data().has("attack")).isFalse();
    assertThat(perona.fruits()).containsExactly("f1");
    assertThat(revision.notes()).isEmpty();
    assertThat(deepSeek.requests).hasSize(1); // nothing new to draft
  }
}
