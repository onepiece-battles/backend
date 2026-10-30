package com.yichi.onepiece.assistant.importing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.model.I18nText;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
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

  /** A Baike lemma card as the card API answers it, trimmed to what the parser reads. */
  private static final String DOFLAMINGO_CARD =
      """
      {"title":"唐吉诃德·多弗朗明哥","desc":"日本漫画《航海王》及其衍生作品中的角色","card":[
        {"name":"中文名","value":["唐吉诃德·多弗朗明哥"]},
        {"name":"外文名","value":["Donquixote Doflamingo","ドンキホーテ·ドフラミンゴ "]},
        {"name":"登场作品","value":["漫画《航海王》及其衍生作品"]},
        {"name":"生日","value":["10月23日(日本)<sup>2</sup><a name=\\"ref_2\\"></a>"]},
        {"name":"年龄","value":["39岁（初登场）→41岁（新世界）<sup>1</sup>"]},
        {"name":"身高","value":["305 cm<sup>1</sup><a name=\\"ref_1\\"></a>"]},
        {"name":"出生地","value":["<a target=_blank href=\\"/item/x\\">玛丽乔亚</a><sup>3</sup>"]},
        {"name":"悬赏金","value":["3亿4000万贝里<sup>4</sup>"]}]}
      """;

  @Test
  public void baikeCardsGiveTheCurrentValueOfTheCharactersOwnEntry() throws Exception {
    var doflamingo = new I18nText().zhCN("唐吉诃德·多弗朗明哥").enUS("Donquixote Doflamingo");
    var facts = BaiduCharacterFacts.parse(DOFLAMINGO_CARD, doflamingo);
    assertThat(facts)
        .containsEntry("生日", "10月23日")
        .containsEntry("年龄", "41岁") // the last step of a timeline is the current value
        .containsEntry("身高", "305 cm")
        .containsEntry("出生地", "玛丽乔亚")
        .containsEntry("悬赏金", "3亿4000万贝里");
    var values = CharacterFactValues.read(facts);
    assertThat(values.get("bounty").asLong()).isEqualTo(340000000L);
    assertThat(values.get("age").asInt()).isEqualTo(41);
    assertThat(values.get("height").asInt()).isEqualTo(305);
    // A short Chinese name still matches through the English one; another name or series does not.
    assertThat(BaiduCharacterFacts.parse(DOFLAMINGO_CARD, new I18nText().zhCN("多弗朗明哥").enUS("Donquixote Doflamingo")))
        .containsKey("身高");
    assertThat(BaiduCharacterFacts.parse(DOFLAMINGO_CARD, new I18nText().zhCN("多弗朗明哥"))).isEmpty();
    assertThat(BaiduCharacterFacts.parse(DOFLAMINGO_CARD.replace("航海王", "其他作品"), doflamingo)).isEmpty();
    assertThat(BaiduCharacterFacts.parse("{}", doflamingo)).isEmpty();
  }

  @Test
  public void aBaikeRedirectFromAnAliasGivesOnlyTheRealName() throws Exception {
    String card =
        """
        {"key":"假路飞","title":"丹马罗·布拉克","desc":"日本漫画《航海王》及其衍生作品中的角色",
         "redirect":{"from":"假路飞","to":"丹马罗·布拉克"},"card":[
          {"name":"中文名","value":["丹马罗·布拉克"]},
          {"name":"外文名","value":["デマロ・ブラック","Demaro Black"]},
          {"name":"身高","value":["180 cm"]}]}
        """;
    assertThat(BaiduCharacterFacts.parse(card, new I18nText().zhCN("假路飞")))
        .containsExactly(
            Map.entry("canonical_zh_CN", "丹马罗·布拉克"), Map.entry("canonical_en_US", "Demaro Black"));
    // Without the redirect another entry is not this character.
    assertThat(BaiduCharacterFacts.parse(card.replace("\"from\":\"假路飞\"", "\"from\":\"别人\""), new I18nText().zhCN("假路飞")))
        .isEmpty();
    // Its own entry gives the names and the facts.
    assertThat(BaiduCharacterFacts.parse(card, new I18nText().zhCN("丹马罗·布拉克")))
        .containsEntry("canonical_zh_CN", "丹马罗·布拉克")
        .containsEntry("身高", "180 cm");
  }

  @Test
  public void theRealNameIsBaikesChineseAndBangumisEnglish() {
    var bangumi = mock(BangumiCharacterFacts.class);
    var baidu = mock(BaiduCharacterFacts.class);
    var fake = new I18nText().zhCN("假路飞");
    when(bangumi.find(fake))
        .thenReturn(Map.of("canonical_zh_CN", "丹马罗·布拉克", "canonical_en_US", "Demaro Black"));
    when(baidu.find(fake)).thenReturn(Map.of("canonical_zh_CN", "德马罗·布莱克"));
    assertThat(new VerifiedCharacterFacts(bangumi, baidu).find(fake))
        .containsEntry("canonical_zh_CN", "德马罗·布莱克")
        .containsEntry("canonical_en_US", "Demaro Black");
  }

  @Test
  public void unclearBaikeValuesStayUnknown() throws Exception {
    String card =
        """
        {"title":"佩罗娜","desc":"《海贼王》中的角色","card":[
          {"name":"身高","value":["160cm","170cm"]},
          {"name":"悬赏金","value":["不明"]},
          {"name":"年龄","value":["约25岁"]},
          {"name":"生日","value":["6月7日"]}]}
        """;
    var facts = BaiduCharacterFacts.parse(card, perona);
    assertThat(facts).doesNotContainKey("身高"); // two different values
    var values = CharacterFactValues.read(facts);
    assertThat(values).containsOnlyKeys("date_of_birth");
  }

  @Test
  public void baikeIsAskedThroughTheCardApiByTheChineseName() {
    var asked = new ArrayList<URI>();
    var baidu =
        new BaiduCharacterFacts(
            uri -> {
              asked.add(uri);
              return DOFLAMINGO_CARD;
            });
    assertThat(baidu.find(new I18nText().zhCN("唐吉诃德·多弗朗明哥"))).containsEntry("年龄", "41岁");
    assertThat(asked)
        .singleElement()
        .satisfies(
            uri -> {
              assertThat(uri.getHost()).isEqualTo("baike.baidu.com");
              assertThat(uri.getPath()).isEqualTo("/api/openapi/BaikeLemmaCardApi");
              assertThat(uri.getQuery()).contains("bk_key=唐吉诃德·多弗朗明哥");
            });
    assertThat(baidu.find(new I18nText().enUS("Doflamingo"))).isEmpty();
    assertThat(asked).hasSize(1); // no Chinese name, no request
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
