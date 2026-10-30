package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;
import static org.assertj.core.api.Assertions.assertThat;

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
}
