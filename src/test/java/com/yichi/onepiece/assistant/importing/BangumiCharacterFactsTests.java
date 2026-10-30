package com.yichi.onepiece.assistant.importing;

import static org.assertj.core.api.Assertions.*;

import com.yichi.onepiece.model.I18nText;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

public class BangumiCharacterFactsTests {
  private static final String MORIA =
      """
      {"id":4287,"name":"ゲッコー・モリア","infobox":[
        {"key":"简体中文名","value":"月光·摩利亚"},
        {"key":"别名","value":[{"v":"月光·莫利亚"},{"v":"Gecko Moria"}]},
        {"key":"生日","value":"9月6日"},{"key":"身高","value":"692cm"},
        {"key":"年龄","value":[{"v":"48岁（第一部）"},{"v":"50岁（第二部）"}]},
        {"key":"性别","value":"男"}]}
      """;
  private static final String SHANKS =
      """
      {"id":2479,"name":"シャンクス","infobox":[
        {"key":"简体中文名","value":"香克斯"},{"key":"别名","value":[{"v":"杰克斯"},{"v":"Shanks"}]},
        {"key":"生日","value":"3月9日"}]}
      """;

  /** Search answers by keyword; every character but 9 is in One Piece. */
  private static BangumiCharacterFacts bangumi(Map<String, String> search, List<String> calls) {
    return new BangumiCharacterFacts(
        (method, path, body) -> {
          calls.add(method + " " + path);
          if (path.startsWith("/v0/search")) {
            for (var entry : search.entrySet())
              if (body.contains(entry.getKey())) return "{\"data\":[" + entry.getValue() + "]}";
            return "{\"data\":[]}";
          }
          return path.contains("/9/") ? "[{\"id\":123}]" : "[{\"id\":975}]";
        });
  }

  @Test
  public void findsTheBirthdayHeightAndAgeOfTheCharacterWithTheDraftedName() {
    var calls = new ArrayList<String>();
    var facts =
        bangumi(Map.of("月光·莫利亚", MORIA), calls).find(new I18nText().zhCN("月光·莫利亚").enUS("Gecko Moria"));
    assertThat(facts)
        .containsExactly(
            Map.entry("生日", "9月6日"), Map.entry("身高", "692cm"), Map.entry("年龄", "48岁（第一部）、50岁（第二部）"));
    assertThat(calls).containsExactly("POST /v0/search/characters?limit=5", "GET /v0/characters/4287/subjects");
  }

  @Test
  public void aLooseHitOrAnotherSeriesGivesNothing() {
    // 杰克斯·莫利亚 finds Shanks (alias 杰克斯) but no drafted name matches him; the English name
    // finds nothing at all.
    var calls = new ArrayList<String>();
    assertThat(bangumi(Map.of("杰克斯", SHANKS), calls).find(new I18nText().zhCN("杰克斯·莫利亚").enUS("Jackson")))
        .isEmpty();
    var otherSeries = MORIA.replace("\"id\":4287", "\"id\":9");
    assertThat(bangumi(Map.of("月光·莫利亚", otherSeries), new ArrayList<>()).find(new I18nText().zhCN("月光·莫利亚")))
        .isEmpty();
  }

  @Test
  public void anUnreachableBangumiGivesNothing() {
    var facts =
        new BangumiCharacterFacts(
            (method, path, body) -> {
              throw new IOException("unreachable");
            });
    assertThat(facts.find(new I18nText().zhCN("佩罗娜"))).isEmpty();
  }
}
