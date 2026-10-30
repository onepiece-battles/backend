package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.testng.annotations.Test;

public class ImportRevisionScopeTests {
  private final ObjectMapper json = new ObjectMapper();

  private ImportItem item(String key, String type, String name) {
    var data = json.createObjectNode();
    data.putObject("name").put("zh_CN", name);
    return new ImportItem(key, type, data, null, List.of(), null, null, List.of(), List.of());
  }

  @Test
  public void separatesTargetsAndCoefficientTypes() {
    var a = item("c1", CHARACTER, "莫利亚");
    var b = item("c2", CHARACTER, "佩罗娜");
    var fruit = item("f1", FRUIT, "灵灵果实");
    var scope =
        new ImportRevisionScope(List.of(a, b, fruit), List.of(), "莫利亚攻击改成900，佩罗娜悬赏留空，灵灵果实攻击系数1.2");
    assertThat(scope.allows(a, "attack")).isTrue();
    assertThat(scope.allows(a, "bounty")).isFalse();
    assertThat(scope.allows(b, "attack")).isFalse();
    assertThat(scope.clears(b, "bounty")).isTrue();
    assertThat(scope.allows(fruit, "attack_slope")).isTrue();
    assertThat(scope.allows(fruit, "attack_intercept")).isFalse();
    assertThat(scope.redraft(b, json.createArrayNode())).isEmpty();
  }

  @Test
  public void additionsIncludeRequiredFruitButExcludeOtherMembersAndGroups() throws Exception {
    var scope = new ImportRevisionScope(List.of(), List.of(), "加上佩罗娜");
    var add =
        json.readTree(
            """
            {"characters":[{"key":"c1","name":{"zh_CN":"佩罗娜"},"fruits":["f1"]},
                           {"key":"c2","name":{"zh_CN":"莫利亚"}}],
             "fruits":[{"key":"f1","name":{"zh_CN":"灵灵果实"}}],
             "groups":[{"key":"g1","name":{"zh_CN":"恐怖三桅帆船"}}]}
            """);
    assertThat(scope.addedKeys(add)).containsExactlyInAnyOrder("c1", "f1");
  }

  @Test
  public void uncertainPronounsDoNotGrantBulkChangesAndTranslationKeepsTheOtherLanguage() {
    var a = item("c1", CHARACTER, "莫利亚");
    var b = item("c2", CHARACTER, "佩罗娜");
    var scope =
        new ImportRevisionScope(List.of(a, b), List.of(new Turn("user", "莫利亚和佩罗娜")), "他的攻击低一点");
    assertThat(scope.allows(a, "attack")).isFalse();
    assertThat(scope.allows(b, "attack")).isFalse();
    scope = new ImportRevisionScope(List.of(a, b), List.of(), "佩罗娜英文名改为Princess Perona");
    assertThat(scope.allows(b, "name")).isTrue();
    assertThat(scope.allowsName("en_US")).isTrue();
    assertThat(scope.allowsName("zh_CN")).isFalse();
  }

  @Test
  public void englishWordsDoNotMatchInsideOtherWords() {
    var a = item("c1", CHARACTER, "莫利亚");
    var scope = new ImportRevisionScope(List.of(a), List.of(), "change c1 attack to 900");
    assertThat(scope.allows(a, "attack")).isTrue();
    assertThat(scope.allows(a, "age")).isFalse(); // "age" inside "change" is not a field
    var fruit = item("f1", FRUIT, "灵灵果实");
    scope = new ImportRevisionScope(List.of(fruit), List.of(), "change f1 attack_slope to 1.2");
    assertThat(scope.allows(fruit, "attack_slope")).isTrue();
    assertThat(scope.allows(fruit, "attack_intercept")).isFalse();
  }

  /** The Donquixote Family: Doflamingo eats the String fruit, Sugar is still uncertain about her bounty. */
  private final List<ImportItem> family =
      List.of(
          member("g1", GROUP, "唐吉诃德家族", null, List.of(), List.of()),
          member("f1", FRUIT, "线线果实", null, List.of(), List.of()),
          member("c1", CHARACTER, "唐吉诃德·多弗朗明哥", "captain", List.of("f1"), List.of()),
          member("c2", CHARACTER, "砂糖", "combatant", List.of(), List.of("bounty")),
          member("c3", CHARACTER, "罗西南迪", "member", List.of(), List.of()),
          member("c4", CHARACTER, "迪亚曼蒂", "combatant", List.of(), List.of()));

  private ImportItem member(
      String key, String type, String name, String position, List<String> fruits, List<String> uncertain) {
    var data = json.createObjectNode();
    data.putObject("name").put("zh_CN", name);
    return new ImportItem(
        key, type, data, null, fruits, position == null ? null : "g1", position, uncertain, List.of());
  }

  private ImportRevisionScope planned(String request, String plan, Turn... history) throws Exception {
    JsonNode node = json.readTree(plan);
    return new ImportRevisionScope(family, List.of(history), request, node);
  }

  private ImportItem in(String key) {
    return family.stream().filter(i -> i.key().equals(key)).findFirst().orElseThrow();
  }

  @Test
  public void aPlanUnderstandsNaturalRequestsTheKeywordsMiss() throws Exception {
    var scope =
        planned(
            "多弗朗明哥太弱了",
            """
            {"targets":[{"key":"c1","action":"change","fields":["attack","defense","blood","speed","intelligence"]}]}
            """);
    assertThat(scope.planned()).isTrue();
    assertThat(scope.allows(in("c1"), "attack")).isTrue();
    assertThat(scope.allows(in("c1"), "energy")).isFalse();
    assertThat(scope.allows(in("c1"), "bounty")).isFalse();
    assertThat(scope.describe()).containsOnlyKeys("c1");

    // Only the one who should be stronger changes; the one compared with does not.
    scope =
        planned(
            "多弗朗明哥是男的，精力应该比砂糖高",
            """
            {"targets":[{"key":"c1","action":"change","fields":["energy"]}]}
            """);
    assertThat(scope.allows(in("c1"), "energy")).isTrue();
    assertThat(scope.allows(in("c2"), "energy")).isFalse();
  }

  @Test
  public void aPlanCannotReachItemsOrFieldsTheRequestDoesNotName() throws Exception {
    // Sugar is not named; the keywords name only the attack for Doflamingo.
    var scope =
        planned(
            "多弗朗明哥的攻击调到1300",
            """
            {"targets":[{"key":"c1","action":"change","fields":["attack","defense","bounty"]},
                        {"key":"c2","action":"change","fields":["attack"]}],
             "remove":["c3"]}
            """);
    assertThat(scope.describe()).containsExactly(org.assertj.core.api.Assertions.entry("c1", java.util.Set.of("attack")));
    assertThat(scope.removes("c3")).isFalse();
    // A negative clause still protects a field the plan lists.
    scope =
        planned(
            "多弗朗明哥再强一点，不要改速度",
            """
            {"targets":[{"key":"c1","action":"change","fields":["attack","speed"]}]}
            """);
    assertThat(scope.allows(in("c1"), "attack")).isTrue();
    assertThat(scope.allows(in("c1"), "speed")).isFalse();
  }

  @Test
  public void pronounsGroupsFruitsAndPositionsReachTheItemsTheyMean() throws Exception {
    var him =
        planned(
            "他太强了，削弱一点",
            """
            {"targets":[{"key":"c1","action":"change","fields":["attack"]},
                        {"key":"c2","action":"change","fields":["attack"]}]}
            """,
            new Turn("user", "多弗朗明哥的攻击调到1300"),
            new Turn("assistant", "已修改 唐吉诃德·多弗朗明哥：攻击。"));
    assertThat(him.allows(in("c1"), "attack")).isTrue();
    assertThat(him.allows(in("c2"), "attack")).isFalse();

    var crew =
        planned(
            "唐吉诃德家族的人精力都高一点",
            """
            {"targets":[{"key":"c2","action":"change","fields":["energy"]},
                        {"key":"c4","action":"change","fields":["energy"]}]}
            """);
    assertThat(crew.allows(in("c2"), "energy")).isTrue();
    assertThat(crew.allows(in("c4"), "energy")).isTrue();

    var fruit =
        planned(
            "多弗朗明哥的果实攻击系数高一点",
            """
            {"targets":[{"key":"f1","action":"change","fields":["attack_slope"]}]}
            """);
    assertThat(fruit.allows(in("f1"), "attack_slope")).isTrue();

    var captain =
        planned(
            "船长的智力再高一点",
            """
            {"targets":[{"key":"c1","action":"change","fields":["intelligence"]},
                        {"key":"c4","action":"change","fields":["intelligence"]}]}
            """);
    assertThat(captain.allows(in("c1"), "intelligence")).isTrue();
    assertThat(captain.allows(in("c4"), "intelligence")).isFalse();
  }

  @Test
  public void lookUpsRelationsClearsAndRemovalsFollowThePlan() throws Exception {
    var lookUp =
        planned(
            "迪亚曼蒂的数据重新查一下",
            """
            {"targets":[{"key":"c4","action":"look_up","fields":["bounty","height","attack"]}]}
            """);
    assertThat(lookUp.lookUps().get("c4")).containsExactlyInAnyOrder("bounty", "height", "attack");
    assertThat(lookUp.describe()).isEmpty(); // nothing for the edit call
    assertThat(lookUp.needsEdit()).isFalse();

    // A wrong fruit is a relation: the edit call changes it.
    var fruit =
        planned(
            "砂糖的果实不对",
            """
            {"targets":[{"key":"c2","action":"look_up","fields":["fruits"]}]}
            """);
    assertThat(fruit.allows(in("c2"), "fruits")).isTrue();
    assertThat(fruit.lookUps()).isEmpty();
    assertThat(fruit.needsEdit()).isTrue();

    // "Leave what is uncertain blank" clears only uncertain fields.
    var blank =
        planned(
            "不确定的都空着",
            """
            {"targets":[{"key":"c2","action":"clear","fields":["bounty"]},
                        {"key":"c4","action":"clear","fields":["attack"]}]}
            """);
    assertThat(blank.clears(in("c2"), "bounty")).isTrue();
    assertThat(blank.clears(in("c4"), "attack")).isFalse();

    var removal =
        planned(
            "罗西南迪不是这个团的，删掉",
            """
            {"targets":[],"remove":["c3","c4"]}
            """);
    assertThat(removal.removals()).containsExactly("c3");
    assertThat(removal.needsEdit()).isFalse();
  }

  @Test
  public void aQuestionChangesNothingAndAnEmptyPlanFallsBackToKeywords() throws Exception {
    var question =
        planned("攻击改300", """
            {"targets":[{"key":"c4","action":"change","fields":["attack"]}],"question":"要改谁的攻击？"}
            """);
    assertThat(question.planned()).isTrue();
    assertThat(question.reply()).isEqualTo("要改谁的攻击？");
    assertThat(question.describe()).isEmpty();
    assertThat(question.needsEdit()).isFalse();

    var fallback = planned("砂糖攻击改300", "{}");
    assertThat(fallback.planned()).isFalse();
    assertThat(fallback.allows(in("c2"), "attack")).isTrue();
    assertThat(fallback.needsEdit()).isTrue();
  }

  @Test
  public void plannedAdditionsAreGroundedByNameOrAnAddingWord() throws Exception {
    var add =
        json.readTree(
            """
            {"characters":[{"key":"n1","name":{"zh_CN":"维尔戈"}},{"key":"n2","name":{"zh_CN":"莫奈"}}]}
            """);
    var named = planned("加上维尔戈", "{\"add\":[{\"type\":\"character\",\"name\":\"维尔戈\"}]}");
    assertThat(named.additions).isTrue();
    assertThat(named.addedKeys(add)).containsExactly("n1");
    var missing =
        planned(
            "还漏了几个干部",
            "{\"add\":[{\"type\":\"character\",\"name\":\"维尔戈\"},{\"type\":\"character\",\"name\":\"莫奈\"}]}");
    assertThat(missing.addedKeys(add)).containsExactlyInAnyOrder("n1", "n2");
    var unasked = planned("多弗朗明哥的攻击调到1300", "{\"add\":[{\"type\":\"character\",\"name\":\"莫奈\"}]}");
    assertThat(unasked.additions).isFalse();
  }

  @Test
  public void aRealNameIsLookedUpForTheCharacterTheRequestMeans() throws Exception {
    var fakes =
        List.of(
            member("c1", CHARACTER, "冒牌路飞", "captain", List.of(), List.of()),
            member("c2", CHARACTER, "冒牌索隆", "combatant", List.of(), List.of()));
    // 假路飞 names 冒牌路飞 only in part; the other impostor is not meant.
    var scope =
        new ImportRevisionScope(
            fakes,
            List.of(),
            "把假路飞改成其真名",
            json.readTree(
                """
                {"targets":[{"key":"c1","action":"look_up","fields":["name"]},
                            {"key":"c2","action":"look_up","fields":["name"]}]}
                """));
    assertThat(scope.nameLookUps()).containsExactly("c1");
    assertThat(scope.describe()).isEmpty(); // the name comes from the sources, not the edit call
    assertThat(scope.needsEdit()).isFalse();
    assertThat(scope.allowsName("zh_CN")).isTrue();
    assertThat(scope.allowsName("en_US")).isTrue();
    // One language when only that one is meant.
    scope =
        new ImportRevisionScope(
            fakes,
            List.of(),
            "冒牌路飞的名字不对",
            json.readTree(
                """
                {"targets":[{"key":"c1","action":"look_up","fields":["name.en_US"]}]}
                """));
    assertThat(scope.allowsName("en_US")).isTrue();
    assertThat(scope.allowsName("zh_CN")).isFalse();
    // Without a word for the name, the plan cannot rename.
    scope =
        new ImportRevisionScope(
            fakes,
            List.of(),
            "冒牌路飞再强一点",
            json.readTree(
                """
                {"targets":[{"key":"c1","action":"look_up","fields":["name","attack"]}]}
                """));
    assertThat(scope.nameLookUps()).isEmpty();
    assertThat(scope.lookUps().get("c1")).containsExactly("attack");
  }

  @Test
  public void aPortraitIsRefreshedOnlyWhenAskedFor() throws Exception {
    String plan =
        """
        {"targets":[{"key":"c2","action":"change","fields":["avatar"]}]}
        """;
    var asked = planned("砂糖换个头像", plan);
    assertThat(asked.portraits()).containsExactly("c2");
    assertThat(asked.needsEdit()).isFalse();
    assertThat(planned("砂糖改名，头像别动", plan).portraits()).isEmpty();
    assertThat(planned("砂糖的英文名改成Sugar", plan).portraits()).isEmpty();
  }

  @Test
  public void nicknamesTheNamesCannotMatchReachTheirItem() throws Exception {
    var crew =
        List.of(
            member("c1", CHARACTER, "乔拉可尔·米霍克", "captain", List.of(), List.of()),
            member("c2", CHARACTER, "马歇尔·D·蒂奇", "combatant", List.of(), List.of()),
            member("c3", CHARACTER, "香克斯", "combatant", List.of(), List.of()));
    var scope =
        new ImportRevisionScope(
            crew,
            List.of(),
            "鹰眼的攻击再高一点，把黑胡子删了",
            json.readTree(
                """
                {"targets":[{"key":"c1","refers_to":"鹰眼","action":"change","fields":["attack"]},
                            {"key":"c3","refers_to":"红发","action":"change","fields":["attack"]}],
                 "remove":[{"key":"c2","refers_to":"黑胡子"}]}
                """));
    assertThat(scope.allows(crew.get(0), "attack")).isTrue();
    assertThat(scope.allows(crew.get(2), "attack")).isFalse(); // 红发 is not in the request
    assertThat(scope.removals()).containsExactly("c2");

    // Words that name another item, or a bare pronoun, do not reach an item.
    scope =
        new ImportRevisionScope(
            crew,
            List.of(),
            "他的攻击和香克斯一样",
            json.readTree(
                """
                {"targets":[{"key":"c1","refers_to":"香克斯","action":"change","fields":["attack"]},
                            {"key":"c2","refers_to":"他","action":"change","fields":["attack"]}]}
                """));
    assertThat(scope.allows(crew.get(0), "attack")).isFalse();
    assertThat(scope.allows(crew.get(1), "attack")).isFalse();
  }
}
