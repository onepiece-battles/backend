package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    assertThat(zoro.fruits()).isEmpty(); // unknown fruit key dropped
    assertThat(zoro.position()).isEqualTo("member"); // unknown position falls back
    assertThat(item(draft, "g1").data().path("group_category").asText()).isEqualTo("pirate");
    assertThat(item(draft, "f2").problems()).isEmpty();
    assertThat(draft.notes()).containsExactly("只包含当前成员");
    assertThat(progress).isNotEmpty();
    var detailRequest = deepSeek.requests.get(1);
    assertThat(detailRequest.jsonOutput()).isTrue();
    assertThat(detailRequest.tools()).isNull();
    assertThat(detailRequest.messages().get(0).path("content").asText())
        .contains("attack: integer 10–1500")
        .contains("\"name\":\"路飞\"");
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
}
