package com.yichi.onepiece.assistant.importing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.model.I18nText;
import java.io.IOException;
import java.util.Map;
import org.testng.annotations.Test;

public class CharacterFactValuesTests {
  private final I18nText perona = new I18nText().zhCN("佩罗娜").enUS("Perona");

  @Test
  public void acceptsOnlySingleExplicitFactsAndNeverSelectsOneNumberFromATimeline() {
    var facts =
        CharacterFactValues.read(
            Map.of("悬赏金", "46亿1110万贝里", "身高", "266cm", "生日", "5月5日", "年龄", "19岁"));
    assertThat(facts.get("bounty").asLong()).isEqualTo(4611100000L);
    assertThat(facts.get("height").asInt()).isEqualTo(266);
    assertThat(facts.get("date_of_birth").asText()).isEqualTo("5月5日");
    assertThat(
            CharacterFactValues.read(
                Map.of("悬赏金", "不明", "年龄", "23岁→25岁", "身高", "约160cm", "生日", "2月31日")))
        .isEmpty();
    assertThat(CharacterFactValues.read(Map.of("悬赏金", "3亿（两年前）→15亿（现在）"))).isEmpty();
    assertThat(CharacterFactValues.read(Map.of("悬赏金", "0"))).isEmpty();
    assertThat(CharacterFactValues.read(Map.of("bounty", "9999999999999999999999999999999")))
        .isEmpty();
  }

  @Test
  public void baikeChecksIdentityAndSeriesAndExtractsOnlyInfoboxFields() {
    String html =
        "<h1>佩罗娜</h1><p>《航海王》中的角色；推测悬赏一亿元</p><dl class=basic-info>"
            + "<dt>身&nbsp;高</dt><dd>160cm<sup>[1]</sup></dd>"
            + "<dt>生日</dt><dd>6月7日</dd><dt>悬赏金</dt><dd>不明</dd></dl>";
    var facts = BaiduCharacterFacts.parse(html, perona);
    assertThat(facts)
        .containsEntry("身高", "160cm")
        .containsEntry("生日", "6月7日")
        .containsEntry("悬赏金", "不明");
    assertThat(CharacterFactValues.read(facts)).doesNotContainKey("bounty");
    assertThat(BaiduCharacterFacts.parse(html.replace("佩罗娜", "同名人物"), perona)).isEmpty();
    assertThat(BaiduCharacterFacts.parse(html.replace("航海王", "其他作品"), perona)).isEmpty();
    assertThat(BaiduCharacterFacts.parse(html + "<dl class=basic-info><dt>身高</dt><dd>170cm</dd></dl>", perona))
        .doesNotContainKey("身高");
  }

  @Test
  public void unavailableSourcesFailSoftAndConflictsStayBlank() {
    var unavailable =
        new BaiduCharacterFacts(
            uri -> {
              throw new IOException("unavailable / 403");
            });
    assertThat(unavailable.find(perona)).isEmpty();
    var bangumi = mock(BangumiCharacterFacts.class);
    var baidu = mock(BaiduCharacterFacts.class);
    when(bangumi.find(perona)).thenReturn(Map.of("身高", "160cm", "生日", "6月7日"));
    when(baidu.find(perona)).thenReturn(Map.of("身高", "170cm", "悬赏金", "7100万贝里"));
    var facts = new VerifiedCharacterFacts(bangumi, baidu).find(perona);
    assertThat(facts)
        .doesNotContainKey("height")
        .containsEntry("bounty", "71000000")
        .containsEntry("date_of_birth", "6月7日");
  }
}
