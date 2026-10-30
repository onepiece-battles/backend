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
import com.yichi.onepiece.model.I18nText;
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
  /** What the fake Bangumi knows, by Chinese name. */
  private final Map<String, Map<String, String>> knownFacts = new HashMap<>();

  /**
   * Answers with {@link #scripted} first; else the roster prompt with {@link #roster}, the fruit
   * check with {@link #fruitCheck} (by default confirming the roster's fruits), a revision with no
   * changes and each detail call with {@link #details}.
   */
  static final class FakeDeepSeek implements ChatCompletionClient {
    String roster;
    /** Models whose calls fail, as an unknown model or an outage would. */
    final Set<String> failing = new HashSet<>();
    Function<JsonNode, String> details = request -> "{\"items\":[]}";
    Function<JsonNode, String> fruitCheck =
        request -> {
          var answer = JSON.createObjectNode();
          var characters = answer.putArray("characters");
          for (var c : request.path("characters")) {
            var listed = c.path("listed_fruits");
            characters
                .addObject()
                .put("key", c.path("key").asText())
                .set("fruit", listed.isEmpty() ? null : listed.get(0));
          }
          return answer.toString();
        };
    final List<CompletionRequest> requests = Collections.synchronizedList(new ArrayList<>());
    final Deque<String> scripted = new ArrayDeque<>();

    @Override
    public Completion complete(CompletionRequest request, Consumer<String> onContentDelta) {
      requests.add(request);
      if (failing.contains(request.model()))
        throw new AssistantException(AssistantException.UPSTREAM_ERROR, "DeepSeek returned HTTP 400");
      String content;
      if (!scripted.isEmpty()) content = scripted.poll();
      else if (request.messages().get(0).path("content").asText().equals(ImportPrompts.REVISE))
        content = "{}";
      else if (request.messages().get(0).path("content").asText().equals(ImportPrompts.ROSTER))
        content = roster;
      else if (request.messages().get(0).path("content").asText().equals(ImportPrompts.FRUITS)) {
        try {
          content =
              fruitCheck.apply(JSON.readTree(request.messages().get(1).path("content").asText()));
        } catch (Exception e) {
          throw new IllegalStateException(e);
        }
      } else {
        try {
          content =
              details.apply(JSON.readTree(request.messages().get(1).path("content").asText()));
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
    knownFacts.clear();
    drafter =
        new ImportDrafter(
            settings,
            deepSeek,
            existing,
            name -> knownFacts.getOrDefault(name.getZhCN(), Map.of()),
            Runnable::run);
    progress.clear();
  }

  private static String character(String key, String extra) {
    return "{\"key\":\"" + key + "\",\"data\":{\"race\":\"HUMAN\",\"battle_style\":\"KATANA\","
        + "\"maximum_fruit_count\":1,\"group_gain\":120,\"attack\":800,\"defense\":500,"
        + "\"blood\":4000,\"speed\":110,\"intelligence\":120,\"energy\":150" + extra + "}}";
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

    assertThat(draft.items())
        .extracting(ImportItem::key)
        .containsExactly("g1", "f1", "f2", "c1", "c2", "c3");
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
    var detailRequest = deepSeek.requests.get(2); // after the roster and the fruit check
    assertThat(deepSeek.requests)
        .extracting(ChatCompletionClient.CompletionRequest::model)
        .containsOnly(ImportDrafter.REASONING_MODEL);
    assertThat(detailRequest.jsonOutput()).isTrue();
    assertThat(detailRequest.tools()).isNull();
    assertThat(detailRequest.messages().get(0).path("content").asText())
        .contains("attack: integer 10–1500")
        .contains("\"name\":\"路飞\"");
  }

  @Test
  public void notesRelationsItCouldNotLinkAndDropsFruitsNobodyAte() {
    when(existing.find(
            eq(FRUIT), argThat(n -> Set.of("影子果实", "透明果实").contains(n.getZhCN())), any()))
        .thenReturn(Optional.of(UUID.randomUUID()));
    // The model names Perona's fruit and crew instead of using their keys, and adds a fruit that
    // no one in the crew eats. The fruit check (here confirming the roster) then drops every
    // fruit nobody holds, the existing one too: it would only have been reused.
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
            "「透明果实」：核对后没有角色吃这个果实，已从草稿去掉；需要的话告诉 AI 它的持有者。",
            "「灵灵果实」：核对后没有角色吃这个果实，已从草稿去掉；需要的话告诉 AI 它的持有者。",
            "「野猪果实」：核对后没有角色吃这个果实，已从草稿去掉；需要的话告诉 AI 它的持有者。");
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
                + character(
                    "c1",
                    ",\"attack\":99999,\"bounty\":\"4,611,100,000\",\"hakis\":[\"Haoshoku\",\"Ryuo\"],\"favourite_food\":\"酒\"")
                + ",{\"key\":\"c2\",\"data\":{\"race\":\"TITAN\",\"attack\":1000},\"uncertain\":[\"attack\"]}]}";

    var draft = drafter.draft("四皇", userId, AssistantLanguage.ZH_CN, progress::add);

    var kaido = item(draft, "c1");
    assertThat(kaido.data().path("attack").asInt()).isEqualTo(1500);
    assertThat(kaido.data().path("bounty").asLong()).isEqualTo(4611100000L); // DeepSeek is sure
    assertThat(kaido.data().has("hakis")).isTrue();
    assertThat(kaido.data().path("hakis")).isEmpty(); // uncertain optional facts stay blank
    assertThat(kaido.data().has("favourite_food")).isFalse();
    assertThat(kaido.uncertain()).contains("attack", "hakis").doesNotContain("bounty");
    assertThat(kaido.problems()).isEmpty();
    var bigMom = item(draft, "c2");
    assertThat(bigMom.data().has("race")).isFalse();
    assertThat(bigMom.uncertain()).contains("race", "attack");
    assertThat(bigMom.problems()).singleElement().asString().contains("race").contains("defense");
  }

  @Test
  public void theFruitCheckAddsMissedFruitsFixesHoldersAndDropsTheRest() {
    // The roster gives Moria Perona's fruit, misses his own, and lists a fruit nobody ate.
    deepSeek.roster =
        """
{"fruits":[{"key":"horo","name":{"zh_CN":"灵灵果实","en_US":"Horo Horo no Mi"},"fruit_type":"Paramecia"},
           {"key":"boar","name":{"zh_CN":"野猪果实"},"fruit_type":"Zoan"}],
 "characters":[{"key":"moria","name":{"zh_CN":"月光·莫利亚","en_US":"Gecko Moria"},"fruits":["horo"]},
               {"key":"perona","name":{"zh_CN":"佩罗娜","en_US":"Perona"}},
               {"key":"hogback","name":{"zh_CN":"霍古巴克","en_US":"Hogback"}}]}
""";
    var asked = new ArrayList<JsonNode>();
    deepSeek.fruitCheck =
        request -> {
          asked.add(request);
          return """
{"characters":[
  {"key":"c1","fruit":{"zh_CN":"影子果实","en_US":"Kage Kage no Mi","fruit_type":"Paramecia"}},
  {"key":"c2","fruit":{"zh_CN":"幽灵果实","en_US":"Horo Horo no Mi"}},
  {"key":"c3","fruit":null}]}
""";
        };
    deepSeek.details =
        request -> {
          var items = new ArrayList<String>();
          request.forEach(
              r ->
                  items.add(
                      r.path("type").asText().equals(FRUIT)
                          ? fruit(r.path("key").asText())
                          : character(r.path("key").asText(), "")));
          return "{\"items\":[" + String.join(",", items) + "]}";
        };

    var draft = drafter.draft("月光莫利亚海贼团", userId, AssistantLanguage.ZH_CN, progress::add);

    assertThat(
            asked
                .get(0)
                .path("characters")
                .get(0)
                .path("listed_fruits")
                .get(0)
                .path("zh_CN")
                .asText())
        .isEqualTo("灵灵果实");
    // Fruits keep the roster's keys; the missed one gets the next key and sits with the fruits.
    assertThat(draft.items())
        .extracting(ImportItem::key)
        .containsExactly("f1", "f3", "c1", "c2", "c3");
    assertThat(item(draft, "c1").fruits()).containsExactly("f3"); // Moria: Kage Kage no Mi
    assertThat(item(draft, "f3").data().path("name").path("en_US").asText())
        .isEqualTo("Kage Kage no Mi");
    assertThat(item(draft, "c2").fruits())
        .containsExactly("f1"); // Perona: matched by romanized name
    assertThat(item(draft, "c3").fruits()).isEmpty();
    assertThat(item(draft, "c1").data().path("maximum_fruit_count").asInt()).isEqualTo(1);
    assertThat(draft.notes())
        .containsExactly("「野猪果实」：核对后没有角色吃这个果实，已从草稿去掉；需要的话告诉 AI 它的持有者。");
    assertThat(progress).contains("正在核对每个角色的果实…");
  }

  @Test
  public void anExistingCharactersFruitIsReusedNotDraftedAgain() {
    UUID moria = UUID.randomUUID(), perona = UUID.randomUUID();
    UUID kage = UUID.randomUUID(), horo = UUID.randomUUID();
    when(existing.find(eq(ImportDto.CHARACTER), argThat(n -> "月光·莫利亚".equals(n.getZhCN())), any()))
        .thenReturn(Optional.of(moria));
    when(existing.find(eq(ImportDto.CHARACTER), argThat(n -> "佩罗娜".equals(n.getZhCN())), any()))
        .thenReturn(Optional.of(perona));
    when(existing.heldFruits(moria))
        .thenReturn(
            List.of(
                new ExistingResources.Held(
                    kage, new I18nText().zhCN("影子果实").enUS("Kage Kage no Mi"))));
    when(existing.heldFruits(perona))
        .thenReturn(List.of(new ExistingResources.Held(horo, new I18nText().zhCN("灵灵果实"))));
    // The draft names Moria's fruit differently and gives Perona none.
    deepSeek.roster =
        """
{"fruits":[{"key":"kage","name":{"zh_CN":"影影果实"},"fruit_type":"Paramecia"},
           {"key":"suke","name":{"zh_CN":"透明果实","en_US":"Suke Suke no Mi"},"fruit_type":"Paramecia"}],
 "characters":[{"key":"moria","name":{"zh_CN":"月光·莫利亚"},"fruits":["kage"]},
               {"key":"perona","name":{"zh_CN":"佩罗娜"}},
               {"key":"absalom","name":{"zh_CN":"阿布萨罗姆"},"fruits":["suke"]}]}
""";
    var asked = new ArrayList<String>();
    deepSeek.details =
        request -> {
          request.forEach(r -> asked.add(r.path("key").asText()));
          return "{\"items\":[" + fruit("f2") + "," + character("c3", "") + "]}";
        };

    var draft = drafter.draft("月光莫利亚海贼团", userId, AssistantLanguage.ZH_CN, progress::add);

    var moriasFruit = item(draft, "f1");
    assertThat(moriasFruit.existingId()).isEqualTo(kage.toString());
    assertThat(moriasFruit.data().path("name").path("zh_CN").asText()).isEqualTo("影子果实");
    assertThat(item(draft, "c1").fruits()).containsExactly("f1");
    var peronasFruit = item(draft, item(draft, "c2").fruits().get(0));
    assertThat(peronasFruit.existingId()).isEqualTo(horo.toString());
    assertThat(item(draft, "c3").fruits()).containsExactly("f2"); // Absalom's new fruit stays new
    assertThat(asked).containsExactlyInAnyOrder("f2", "c3"); // nothing reused is drafted
    assertThat(draft.notes()).containsExactly("「影影果实」就是已有的「影子果实」，直接复用。");
  }

  @Test
  public void aReplyThatIsNotJsonIsRetriedOnceThenReported() {
    deepSeek.roster =
        "{\"groups\":[{\"key\":\"g\",\"name\":{\"zh_CN\":\"海军\"},\"group_category\":\"marine\"}]}";
    deepSeek.scripted.add("Sure! Here is the roster:");
    var draft = drafter.draft("海军", userId, AssistantLanguage.ZH_CN, progress::add);
    assertThat(draft.items()).extracting(ImportItem::key).containsExactly("g1");

    // Twice from the thinking model, then twice from the chat model standing in.
    for (int i = 0; i < 4; i++) deepSeek.scripted.add("not json");
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
    assertThatThrownBy(
            () -> drafter.draft("nobody", userId, AssistantLanguage.EN_US, progress::add))
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
    data.set(
        "name",
        JSON.createObjectNode().put("zh_CN", "月光·莫利亚海贼团").put("en_US", "Thriller Bark Pirates"));
    return drafted("g1", GROUP, data, List.of(), null, null);
  }

  /** Doflamingo (eating a wrongly drafted fruit f1) and Sugar, both new. */
  private List<ImportItem> family() throws Exception {
    return List.of(
        drafted("f1", FRUIT, data(fruit("f1"), "糖糖果实", "Ame Ame no Mi"), List.of(), null, null),
        drafted(
            "c1",
            ImportDto.CHARACTER,
            data(character("c1", ",\"bounty\":340000000"), "唐吉诃德·多弗朗明哥", "Donquixote Doflamingo"),
            List.of(),
            null,
            null),
        drafted(
            "c2",
            ImportDto.CHARACTER,
            data(character("c2", ""), "砂糖", "Sugar"),
            List.of("f1"),
            null,
            null));
  }

  /** The fake Straw Hats' captain, drafted under his nickname, and his crewmate. */
  private List<ImportItem> impostors() throws Exception {
    return List.of(
        drafted(
            "c1",
            ImportDto.CHARACTER,
            data(character("c1", ",\"height\":180"), "假路飞", "Fake Luffy"),
            List.of(),
            null,
            null),
        drafted(
            "c2",
            ImportDto.CHARACTER,
            data(character("c2", ""), "假索隆", "Fake Zoro"),
            List.of(),
            null,
            null));
  }

  private ImportRevision revise(List<ImportItem> current, String request, Turn... history) {
    return drafter.revise(current, List.of(history), request, userId, AssistantLanguage.ZH_CN, progress::add);
  }

  @Test
  public void oneThinkingCallRevisesOnlyWhatTheRequestReachesAndKeepsTheReviewersEdits()
      throws Exception {
    // The reviewer already set Moria's bounty by hand.
    var current =
        List.of(
            crew(),
            drafted(
                "f1", FRUIT, data(fruit("f1"), "影子果实", "Kage Kage no Mi"), List.of(), null, null),
            drafted(
                "c1",
                ImportDto.CHARACTER,
                data(character("c1", ",\"bounty\":320000000"), "月光·莫利亚", "Gecko Moria"),
                List.of("f1"),
                "g1",
                "captain"),
            drafted(
                "c2",
                ImportDto.CHARACTER,
                data(character("c2", ""), "霍古巴克", "Hogback"),
                List.of(),
                "g1",
                "member"));
    // The answer strays to Moria, whom the request does not mention.
    deepSeek.scripted.add(
        """
{"remove":[{"key":"c2","refers_to":"霍古巴克"}],
 "changes":[{"key":"c1","refers_to":"莫利亚","set":{"speed":999,"name":{"en_US":"Gecko Moria (Warlord)"}}}],
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

    var revision =
        revise(
            current,
            "去掉霍古巴克，加上佩罗娜",
            new Turn("user", "月光·莫利亚海贼团"),
            new Turn("assistant", "起草了 1 个团体、1 个果实、2 个角色。"));

    assertThat(revision.reply()).isEqualTo("已新增 灵灵果实、佩罗娜。已移除 霍古巴克。其他内容未改动。");
    // A removed key is never handed out again: Perona is c3, not c2.
    assertThat(revision.items()).extracting(ImportItem::key).containsExactly("g1", "f1", "c1", "f2", "c3");
    assertThat(revision.changed()).containsExactly("f2", "c3");
    assertThat(asked).containsExactlyInAnyOrder("f2", "c3");
    var moria = revision.items().get(2);
    assertThat(moria.data()).isEqualTo(current.get(2).data()); // speed, name and bounty untouched
    var perona = revision.items().get(4);
    assertThat(perona.fruits()).containsExactly("f2");
    assertThat(perona.group()).isEqualTo("g1");
    assertThat(perona.position()).isEqualTo("combatant");
    assertThat(revision.notes()).containsExactly("佩罗娜是幽灵公主");

    var call = deepSeek.requests.get(0);
    assertThat(call.model()).isEqualTo(ImportDrafter.REASONING_MODEL);
    assertThat(call.maxTokens()).isEqualTo(ImportDrafter.REASONING_MAX_TOKENS);
    assertThat(call.messages()).extracting(m -> m.path("role").asText())
        .containsExactly("system", "user", "assistant", "user");
    assertThat(call.messages().get(0).path("content").asText()).isEqualTo(ImportPrompts.REVISE);
    assertThat(call.messages().get(3).path("content").asText())
        .contains("Request: 去掉霍古巴克，加上佩罗娜", "\"key\":\"c2\"", "320000000", "\"name\":\"路飞\"");
  }

  @Test
  public void deepSeekFindsFactsAndTheSourcesCorrectThem() throws Exception {
    var current =
        List.of(
            drafted(
                "c1",
                ImportDto.CHARACTER,
                data(character("c1", ",\"bounty\":1000,\"height\":160"), "佩罗娜", "Perona"),
                List.of(),
                null,
                null));
    knownFacts.put("佩罗娜", Map.of("悬赏金", "7100万贝里"));
    // Sure of the bounty and the birthday, unsure of the age; the height is not asked for.
    deepSeek.scripted.add(
        """
        {"changes":[{"key":"c1","refers_to":"佩罗娜",
                     "set":{"bounty":99000000,"date_of_birth":"6月7日","age":null,"height":999},
                     "unsure":["age"]}]}
        """);
    var revision = revise(current, "佩罗娜的资料不对，查一下");
    var perona = revision.items().get(0);
    assertThat(perona.data().path("bounty").asLong()).isEqualTo(71000000); // Baike wins
    assertThat(perona.data().path("date_of_birth").asText()).isEqualTo("6月7日"); // DeepSeek's
    assertThat(perona.data().has("age")).isFalse();
    assertThat(perona.uncertain()).contains("age");
    assertThat(perona.data().path("height").asInt()).isEqualTo(999); // 资料 covers height
    assertThat(perona.data().path("attack").asInt()).isEqualTo(800);
    assertThat(revision.reply()).contains("没有可核实的来源，保持留空：佩罗娜 年龄");

    // A value the reviewer gives stands, whatever the sources say.
    deepSeek.scripted.add(
        "{\"changes\":[{\"key\":\"c1\",\"refers_to\":\"佩罗娜\",\"set\":{\"bounty\":400000000,\"height\":999}}]}");
    var given = revise(current, "佩罗娜悬赏改成4亿").items().get(0);
    assertThat(given.data().path("bounty").asLong()).isEqualTo(400000000);
    assertThat(given.data().path("height").asInt()).isEqualTo(160); // only the bounty was asked for

    // A blank asked for is a blank, not a look-up.
    deepSeek.scripted.add(
        "{\"changes\":[{\"key\":\"c1\",\"refers_to\":\"佩罗娜\",\"set\":{\"bounty\":null}}]}");
    deepSeek.requests.clear();
    var cleared = revise(current, "佩罗娜悬赏留空").items().get(0);
    assertThat(cleared.data().has("bounty")).isFalse();
    assertThat(deepSeek.requests).hasSize(1);
  }

  @Test
  public void aNaturalRequestChangesOnlyItsSubject() throws Exception {
    var current = family();
    deepSeek.scripted.add(
        """
        {"changes":[{"key":"c1","refers_to":"多弗朗明哥","set":{"energy":220}}]}
        """);
    var revision = revise(current, "多弗朗明哥是男的，精力应该比砂糖高");
    assertThat(revision.items().get(1).data().path("energy").asInt()).isEqualTo(220);
    assertThat(revision.items().get(2).data()).isEqualTo(current.get(2).data());
    assertThat(revision.changed()).containsExactly("c1");
    assertThat(revision.reply()).isEqualTo("已修改 唐吉诃德·多弗朗明哥：精力。其他内容未改动。");
  }

  @Test
  public void protectedFieldsAndItemsTheRequestDoesNotMeanStayUntouched() throws Exception {
    var current = family();
    deepSeek.scripted.add(
        """
        {"changes":[{"key":"c2","refers_to":"砂糖","set":{"attack":500,"speed":150}}],
         "remove":[{"key":"c1","refers_to":"多弗朗明哥"}]}
        """);
    var revision = revise(current, "砂糖攻击改500，不要改速度");
    assertThat(revision.items()).hasSize(3);
    assertThat(revision.items().get(2).data().path("attack").asInt()).isEqualTo(500);
    assertThat(revision.items().get(2).data().path("speed").asInt()).isEqualTo(110);

    // Field words are not a reference to an item.
    deepSeek.scripted.add(
        "{\"changes\":[{\"key\":\"c2\",\"refers_to\":\"攻击\",\"set\":{\"attack\":300}}]}");
    var unclear = revise(current, "攻击改300");
    assertThat(unclear.changed()).isEmpty();
    assertThat(unclear.reply()).isEqualTo("草稿未变更。请说明要改哪一项和改什么。");
  }

  @Test
  public void aQuestionOrAnAnswerChangesNothing() throws Exception {
    var current = family();
    deepSeek.scripted.add("{\"question\":\"你想调整谁的攻击？多弗朗明哥还是砂糖？\"}");
    var revision = revise(current, "攻击改300");
    assertThat(revision.reply()).isEqualTo("你想调整谁的攻击？多弗朗明哥还是砂糖？");
    assertThat(revision.changed()).isEmpty();
    assertThat(revision.items()).extracting(ImportItem::data)
        .containsExactlyElementsOf(current.stream().map(ImportItem::data).toList());
  }

  @Test
  public void aWrongFruitIsReplacedAndTheOldOneLeavesTheDraft() throws Exception {
    var current = family();
    deepSeek.scripted.add(
        """
        {"changes":[{"key":"c2","refers_to":"砂糖","fruits":["new_f1"]}],
         "add":{"fruits":[{"key":"new_f1","name":{"zh_CN":"童趣果实","en_US":"Hobi Hobi no Mi"},"fruit_type":"Paramecia"}]}}
        """);
    deepSeek.details = request -> "{\"items\":[" + fruit("f2") + "]}";

    var revision = revise(current, "砂糖的果实不对");

    assertThat(revision.items()).extracting(ImportItem::key).containsExactly("c1", "c2", "f2");
    assertThat(revision.items().get(1).fruits()).containsExactly("f2");
    assertThat(revision.items().get(2).data().path("name").path("zh_CN").asText()).isEqualTo("童趣果实");
    assertThat(revision.notes()).containsExactly("「糖糖果实」不再有人吃，已从草稿去掉。");
    assertThat(revision.reply()).isEqualTo("已修改 砂糖：果实。已新增 童趣果实。已移除 糖糖果实。其他内容未改动。");
  }

  @Test
  public void anExistingItemOnlyChangesItsRelationsAndANewCharacterHoldsOneFruit() throws Exception {
    var name = JSON.createObjectNode();
    name.set("name", JSON.createObjectNode().put("zh_CN", "佩罗娜").put("en_US", "Perona"));
    var current =
        List.of(
            drafted("f1", FRUIT, data(fruit("f1"), "灵灵果实", "Horo Horo no Mi"), List.of(), null, null),
            new ImportItem(
                "c1", ImportDto.CHARACTER, name, UUID.randomUUID().toString(), List.of(), null, null,
                List.of(), List.of()));
    deepSeek.scripted.add(
        "{\"changes\":[{\"key\":\"c1\",\"refers_to\":\"佩罗娜\",\"set\":{\"attack\":100},\"fruits\":[\"f1\"]}]}");
    var revision = revise(current, "灵灵果实给佩罗娜");
    assertThat(revision.items().get(1).data().has("attack")).isFalse();
    assertThat(revision.items().get(1).fruits()).containsExactly("f1");
    assertThat(deepSeek.requests).hasSize(1); // nothing new to draft

    var two =
        List.of(
            drafted("f1", FRUIT, data(fruit("f1"), "暗暗果实", "Yami Yami no Mi"), List.of(), null, null),
            drafted("f2", FRUIT, data(fruit("f2"), "震震果实", "Gura Gura no Mi"), List.of(), null, null),
            drafted("c1", ImportDto.CHARACTER, data(character("c1", ""), "测试角色", "Test"), List.of("f1"), null, null));
    deepSeek.scripted.add(
        "{\"changes\":[{\"key\":\"c1\",\"refers_to\":\"他\",\"fruits\":[\"f1\",\"f2\"],\"set\":{\"maximum_fruit_count\":2}}]}");
    var character = revise(two, "两个都给他").items().get(2);
    assertThat(character.data().path("maximum_fruit_count").asInt()).isEqualTo(1);
    assertThat(character.problems()).containsExactly("新角色只能吃 1 个果实，请去掉多余的。");
  }

  @Test
  public void aFollowUpRenamesTheNextImpostorWithDeepSeeksAnswer() throws Exception {
    var current = impostors();
    deepSeek.scripted.add(
        """
        {"changes":[{"key":"c2","refers_to":"假索隆","set":{"name":{"zh_CN":"曼加罗","en_US":"Manjaro"}},
                     "impersonates":{"zh_CN":"罗罗诺亚·索隆","en_US":"Roronoa Zoro"}}]}
        """);

    var revision =
        revise(
            current,
            "假索隆也一样",
            new Turn("user", "把假路飞改成其真名"),
            new Turn("assistant", "已修改 假路飞 → 丹马罗·布拉克：名字。其他内容未改动。"));

    var manjaro = revision.items().get(1);
    assertThat(manjaro.data().path("name").path("zh_CN").asText()).isEqualTo("曼加罗");
    assertThat(manjaro.data().path("name").path("en_US").asText()).isEqualTo("Manjaro");
    var unchanged = manjaro.data().deepCopy();
    unchanged.set("name", current.get(1).data().get("name"));
    assertThat(unchanged).isEqualTo(current.get(1).data()); // only the name
    assertThat(revision.items().get(0).data()).isEqualTo(current.get(0).data());
    assertThat(revision.refreshAvatars()).isEmpty();
    assertThat(revision.reply()).isEqualTo("已修改 假索隆 → 曼加罗：名字。其他内容未改动。");
    assertThat(revision.notes()).containsExactly("「假索隆」的真名来自 DeepSeek，百度百科 / Bangumi 里没有收录，可以再确认一下。");
  }

  @Test
  public void theSourcesSpellARealNameOrKnowTheAlias() throws Exception {
    var current = impostors();
    // DeepSeek's spelling is respelled as Bangumi writes it.
    knownFacts.put("德马罗·布莱克", Map.of("canonical_zh_CN", "丹马罗·布拉克", "canonical_en_US", "Demaro Black"));
    deepSeek.scripted.add(
        """
        {"changes":[{"key":"c1","refers_to":"假路飞","set":{"name":{"zh_CN":"德马罗·布莱克","en_US":"Demaro Black"}}}]}
        """);
    assertThat(revise(current, "把假路飞改成其真名").items().get(0).data().path("name").path("zh_CN").asText())
        .isEqualTo("丹马罗·布拉克");

    // Without an answer from DeepSeek, a source that knows the drafted name as an alias names him.
    knownFacts.put("假路飞", Map.of("canonical_zh_CN", "丹马罗·布拉克", "canonical_en_US", "Demaro Black"));
    deepSeek.scripted.add(
        "{\"changes\":[{\"key\":\"c1\",\"refers_to\":\"假路飞\",\"set\":{\"name\":null},\"unsure\":[\"name\"]}]}");
    assertThat(revise(current, "把假路飞改成其真名").items().get(0).data().path("name").path("en_US").asText())
        .isEqualTo("Demaro Black");

    // A name the reviewer gives is taken as written.
    knownFacts.put("曼加罗", Map.of("canonical_zh_CN", "曼佳罗"));
    deepSeek.scripted.add(
        "{\"changes\":[{\"key\":\"c2\",\"refers_to\":\"假索隆\",\"set\":{\"name\":{\"zh_CN\":\"曼加罗\"}}}]}");
    var given = revise(current, "假索隆的中文名改成曼加罗").items().get(1).data().path("name");
    assertThat(given.path("zh_CN").asText()).isEqualTo("曼加罗");
    assertThat(given.path("en_US").asText()).isEqualTo("Fake Zoro");

    // An English fix keeps the Chinese name.
    deepSeek.scripted.add(
        "{\"changes\":[{\"key\":\"c1\",\"refers_to\":\"假路飞\",\"set\":{\"name\":{\"zh_CN\":\"丹马罗·布拉克\",\"en_US\":\"Demaro Black\"}}}]}");
    var english = revise(current, "假路飞的英文名写错了").items().get(0).data().path("name");
    assertThat(english.path("zh_CN").asText()).isEqualTo("假路飞");
    assertThat(english.path("en_US").asText()).isEqualTo("Demaro Black");
  }

  @Test
  public void anImpostorNeverBecomesTheOneHeImpersonates() throws Exception {
    var current =
        List.of(
            drafted(
                "c1",
                ImportDto.CHARACTER,
                data(character("c1", ""), "蒙奇·D·路飞", "Monkey D. Luffy"),
                List.of(),
                null,
                null));
    deepSeek.scripted.add(
        """
        {"changes":[{"key":"c1","refers_to":"这个路飞","set":{"name":{"zh_CN":"蒙奇·D·路飞"}},
                     "impersonates":{"zh_CN":"蒙奇·D·路飞"}}]}
        """);
    var refused = revise(current, "这个路飞是假的，改成他自己的真名");
    assertThat(refused.changed()).isEmpty();
    assertThat(refused.reply()).isEqualTo("「蒙奇·D·路飞」：「蒙奇·D·路飞」是另一个角色的名字，没有采用。");

    // Drafted under Luffy's own name, the sources describe the real Luffy.
    knownFacts.put("蒙奇·D·路飞", Map.of("canonical_zh_CN", "蒙奇·D·路飞", "canonical_en_US", "Monkey D. Luffy"));
    deepSeek.scripted.add(
        """
        {"changes":[{"key":"c1","refers_to":"这个路飞","set":{"name":null},"unsure":["name"],
                     "impersonates":{"zh_CN":"蒙奇·D·路飞"}}]}
        """);
    var unknown = revise(current, "这个路飞是假的，改成他自己的真名");
    assertThat(unknown.changed()).isEmpty();
    assertThat(unknown.reply())
        .isEqualTo("「蒙奇·D·路飞」：DeepSeek 和百度百科 / Bangumi 都没有给出可靠的真实姓名，名字保持不变。");
  }

  @Test
  public void aPortraitIsLookedUpAgainOnlyWhenAskedFor() throws Exception {
    var current = impostors();
    deepSeek.scripted.add("{\"changes\":[{\"key\":\"c2\",\"refers_to\":\"假索隆\",\"avatar\":true}]}");
    var revision = revise(current, "假索隆换个头像");
    assertThat(revision.refreshAvatars()).containsExactly("c2");
    assertThat(revision.changed()).containsExactly("c2");
    assertThat(revision.reply()).isEqualTo("重新查找头像：假索隆。其他内容未改动。");

    deepSeek.scripted.add("{\"changes\":[{\"key\":\"c2\",\"refers_to\":\"假索隆\",\"avatar\":true}]}");
    assertThat(revise(current, "假索隆的头像别动").refreshAvatars()).isEmpty();
  }

  @Test
  public void theThinkingModelGetsAnAlternatingConversation() throws Exception {
    revise(
        family(),
        "多弗朗明哥精力高一点",
        new Turn("assistant", "起草了 1 个果实、2 个角色。"),
        new Turn("user", "多弗朗明哥攻击低一点"),
        new Turn("user", "多弗朗明哥攻击低一点"),
        new Turn("assistant", "草稿未变更。"));
    var messages = deepSeek.requests.get(0).messages();
    assertThat(messages).extracting(m -> m.path("role").asText())
        .containsExactly("system", "user", "assistant", "user");
    assertThat(messages.get(1).path("content").asText()).isEqualTo("多弗朗明哥攻击低一点\n\n多弗朗明哥攻击低一点");
  }

  @Test
  public void withoutTheThinkingModelTheChatModelAnswers() throws Exception {
    var current = family();
    deepSeek.failing.add(ImportDrafter.REASONING_MODEL);
    deepSeek.scripted.add("{\"changes\":[{\"key\":\"c1\",\"refers_to\":\"多弗朗明哥\",\"set\":{\"energy\":220}}]}");
    var revision = revise(current, "多弗朗明哥精力高一点");
    assertThat(deepSeek.requests)
        .extracting(ChatCompletionClient.CompletionRequest::model)
        .containsExactly(ImportDrafter.REASONING_MODEL, "deepseek-chat");
    assertThat(revision.items().get(1).data().path("energy").asInt()).isEqualTo(220);
  }

  @Test
  public void removalsAndAdditionsNeedTheRequest() throws Exception {
    var current =
        List.of(
            crew(),
            drafted(
                "f1", FRUIT, data(fruit("f1"), "影子果实", "Kage Kage no Mi"), List.of(), null, null),
            drafted(
                "c1",
                ImportDto.CHARACTER,
                data(character("c1", ""), "月光·莫利亚", "Gecko Moria"),
                List.of("f1"),
                "g1",
                "captain"));
    // Moria goes with his fruit; Absalom's fruit is not a key, and the crew is not meant.
    deepSeek.scripted.add(
        """
        {"remove":[{"key":"c1","refers_to":"莫利亚"},{"key":"g1","refers_to":"莫利亚"}],
         "changes":[{"key":"c9","refers_to":"莫利亚","set":{"attack":100}}],
         "add":{"characters":[{"key":"absalom","name":{"zh_CN":"阿布萨罗姆","en_US":"Absalom"},
                               "fruits":["透明果实"],"group":"g1"}]}}
        """);
    deepSeek.details = request -> "{\"items\":[" + character("c2", "") + "]}";

    var revision = revise(current, "把莫利亚换成阿布萨罗姆");

    assertThat(revision.items()).extracting(ImportItem::key).containsExactly("g1", "c2");
    var absalom = revision.items().get(1);
    assertThat(absalom.fruits()).isEmpty();
    assertThat(absalom.group()).isEqualTo("g1");
    assertThat(revision.notes()).containsExactly("「阿布萨罗姆」的果实「透明果实」不在名单里，已忽略，请确认。");

    // An addition the request does not ask for is left out.
    deepSeek.scripted.add(
        """
        {"add":{"characters":[{"key":"x","name":{"zh_CN":"霍古巴克"}}]}}
        """);
    assertThat(revise(current, "莫利亚的攻击不变").items()).hasSize(3);
  }
}
