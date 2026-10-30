package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.yichi.onepiece.model.I18nText;
import java.util.List;
import java.util.UUID;
import org.testng.annotations.Test;

public class ImportRevisionScopeTests {
  private final ObjectMapper json = new ObjectMapper();

  /** The Donquixote Family: Doflamingo eats the String fruit and is its captain. */
  private final List<ImportItem> family =
      List.of(
          member("g1", GROUP, "唐吉诃德家族", null, List.of()),
          member("f1", FRUIT, "线线果实", null, List.of()),
          member("c1", CHARACTER, "唐吉诃德·多弗朗明哥", "captain", List.of("f1")),
          member("c2", CHARACTER, "砂糖", "combatant", List.of()),
          member("c3", CHARACTER, "冒牌路飞", "member", List.of()),
          member("c4", CHARACTER, "乔拉可尔·米霍克", "member", List.of()));

  private ImportItem member(
      String key, String type, String name, String position, List<String> fruits) {
    var data = json.createObjectNode();
    data.putObject("name").put("zh_CN", name);
    return new ImportItem(
        key, type, data, null, fruits, position == null ? null : "g1", position, List.of(), List.of());
  }

  private ImportItem in(String key) {
    return family.stream().filter(i -> i.key().equals(key)).findFirst().orElseThrow();
  }

  private ImportRevisionScope scope(String request, Turn... history) {
    return new ImportRevisionScope(family, List.of(history), request);
  }

  private static TextNode words(String text) {
    return TextNode.valueOf(text);
  }

  @Test
  public void namesNicknamesAndDescriptionsReachTheirItem() {
    var scope = scope("鹰眼的攻击再高一点");
    assertThat(scope.reaches(in("c4"), words("鹰眼"))).isTrue(); // the model's reading of a nickname
    assertThat(scope.reaches(in("c4"), words("红发"))).isFalse(); // not in the request
    assertThat(scope.reaches(in("c2"), null)).isFalse();

    scope = scope("把假路飞改成其真名");
    assertThat(scope.reaches(in("c3"), null)).isTrue(); // part of 冒牌路飞

    scope = scope("他的攻击和砂糖一样");
    assertThat(scope.reaches(in("c1"), words("砂糖"))).isFalse(); // names another item
    assertThat(scope.reaches(in("c1"), words("他"))).isFalse(); // a pronoun needs the conversation

    scope = scope("攻击改300");
    assertThat(scope.reaches(in("c2"), words("攻击"))).isFalse(); // a field is no reference
  }

  @Test
  public void pronounsCrewsFruitsPositionsAndAllWordsReachTheItemsTheyMean() {
    var him =
        scope(
            "他太强了，削弱一点",
            new Turn("user", "多弗朗明哥的攻击调到1300"),
            new Turn("assistant", "已修改 唐吉诃德·多弗朗明哥：攻击。"));
    assertThat(him.reaches(in("c1"), words("他"))).isTrue();
    assertThat(him.reaches(in("c2"), words("他"))).isFalse();

    var crew = scope("唐吉诃德家族的人精力都高一点");
    assertThat(crew.reaches(in("c2"), null)).isTrue();
    var fruit = scope("多弗朗明哥的果实攻击系数高一点");
    assertThat(fruit.reaches(in("f1"), null)).isTrue();
    var captain = scope("船长的智力再高一点");
    assertThat(captain.reaches(in("c1"), null)).isTrue();
    assertThat(captain.reaches(in("c2"), null)).isFalse();
    var everyone = scope("所有人的精力都高一点");
    assertThat(everyone.reaches(in("c4"), null)).isTrue();
  }

  @Test
  public void aRequestAboutOneItemBoundsItsFieldsAndNegativeClausesProtect() {
    var scope = scope("砂糖的攻击改成900");
    assertThat(scope.allows(in("c2"), "attack")).isTrue();
    assertThat(scope.allows(in("c2"), "defense")).isFalse();
    // Natural requests name no field: what changes is the model's reading.
    scope = scope("多弗朗明哥太弱了");
    assertThat(scope.allows(in("c1"), "defense")).isTrue();
    // About several items, which field is whose is the model's reading too.
    scope = scope("多弗朗明哥是男的，精力应该比砂糖高");
    assertThat(scope.allows(in("c1"), "energy")).isTrue();
    // What a negative clause names stays.
    scope = scope("多弗朗明哥再强一点，不要改速度");
    assertThat(scope.allows(in("c1"), "attack")).isTrue();
    assertThat(scope.allows(in("c1"), "speed")).isFalse();
    scope = scope("砂糖攻击改500，多弗朗明哥别动");
    assertThat(scope.allows(in("c1"), "attack")).isFalse();
    assertThat(scope.allows(in("c2"), "attack")).isTrue();
    // A fruit's stats are its coefficients.
    scope = scope("多弗朗明哥的果实攻击系数高一点");
    assertThat(scope.allows(in("f1"), "attack_slope")).isTrue();
    assertThat(scope.allows(in("f1"), "attack_intercept")).isFalse();
  }

  @Test
  public void anExistingItemOnlyChangesRelationsAndAPortraitNeedsAnAsk() {
    var data = json.createObjectNode();
    data.putObject("name").put("zh_CN", "佩罗娜");
    var existing =
        new ImportItem(
            "c9", CHARACTER, data, UUID.randomUUID().toString(), List.of(), null, null, List.of(),
            List.of());
    var scope = new ImportRevisionScope(List.of(existing), List.of(), "佩罗娜吃灵灵果实，攻击高一点");
    assertThat(scope.allows(existing, "fruits")).isTrue();
    assertThat(scope.allows(existing, "attack")).isFalse();

    assertThat(scope("砂糖换个头像").allows(in("c2"), "avatar")).isTrue();
    assertThat(scope("砂糖改名，头像别动").allows(in("c2"), "avatar")).isFalse();
    assertThat(scope("砂糖的英文名改成Sugar").allows(in("c2"), "avatar")).isFalse();
  }

  @Test
  public void namesChangeInTheLanguageMeantAndAdditionsNeedAnAsk() {
    assertThat(scope("砂糖的英文名改成Sugar").allowsName("zh_CN")).isFalse();
    assertThat(scope("砂糖的英文名改成Sugar").allowsName("en_US")).isTrue();
    assertThat(scope("把假路飞改成其真名").allowsName("zh_CN")).isTrue();

    var vergo = new I18nText().zhCN("维尔戈");
    assertThat(scope("加上维尔戈").allowsAddition(vergo)).isTrue();
    assertThat(scope("还漏了几个干部").allowsAddition(vergo)).isTrue();
    assertThat(scope("多弗朗明哥的攻击调到1300").allowsAddition(vergo)).isFalse();
  }
}
