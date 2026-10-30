package com.yichi.onepiece.assistant.importing;

import static org.assertj.core.api.Assertions.*;

import com.yichi.onepiece.model.I18nText;
import org.testng.annotations.Test;

public class ExistingResourcesTests {
  @Test
  public void fruitNameVariantsAreTheSameFruit() {
    assertThat(ExistingResources.fruitCore("Gomu-Gomu no Mi")).isEqualTo("gomugomu");
    assertThat(ExistingResources.fruitCore("gomu gomu no mi")).isEqualTo("gomugomu");
    assertThat(ExistingResources.fruitCore("橡胶果实")).isEqualTo("橡胶");
    assertThat(
            ExistingResources.sameFruit(
                new I18nText().zhCN("橡胶果实").enUS("Gomu Gomu no Mi"),
                new I18nText().enUS("Gomu-Gomu no Mi")))
        .isTrue();
    assertThat(
            ExistingResources.sameFruit(
                new I18nText().zhCN("影子果实"), new I18nText().zhCN("影子 果实")))
        .isTrue();
    assertThat(
            ExistingResources.sameFruit(
                new I18nText().enUS("Mera Mera no Mi"), new I18nText().enUS("Magu Magu no Mi")))
        .isFalse();
  }
}
