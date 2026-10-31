package com.yichi.onepiece.battle.skill;

import static com.yichi.onepiece.battle.skill.CombatModels.*;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.fruit.model.*;
import com.yichi.onepiece.model.I18nText;
import java.util.*;
import org.testng.annotations.Test;

public class CanonCatalogueTests {
  private final CombatCatalog catalog = new CombatCatalog();

  private Profile profile(String identity) {
    return catalog.catalog().characters().stream()
        .filter(c -> c.code().equals(identity))
        .findFirst()
        .orElseThrow()
        .profile();
  }

  private Skill skill(String identity, String suffix) {
    return profile(identity).skills().stream()
        .filter(s -> s.id().equals(identity + "." + suffix))
        .findFirst()
        .orElseThrow();
  }

  @Test
  public void researchedLoadoutsContainNoGeneratedFollowupsOrInventedCounterNames() {
    assertThat(catalog.catalog().version()).isEqualTo(2);
    for (var entry : catalog.catalog().characters()) {
      assertThat(entry.profile().version()).isEqualTo(2);
      assertThat(entry.profile().identity()).isEqualTo(entry.code());
      assertThat(entry.profile().era()).contains("2026-10");
      for (var skill : entry.profile().skills()) {
        assertThat(skill.name()).doesNotContain("·追击", "·反击");
        assertThat(skill.englishName()).doesNotContain("signature", "follow-up");
        assertThat(skill.evidence())
            .allSatisfy(
                e -> {
                  assertThat(e.url()).startsWith("https://one-piece.com/");
                  assertThat(e.note()).contains("2026-10-31", "游戏参数");
                });
      }
    }
    assertThat(skill("luffy", "white-star-gun").name()).contains("白星枪");
    assertThat(skill("zoro", "hahava-leopard-hunt").name()).contains("彪狩");
    assertThat(skill("garp", "galaxy-impact").name()).isEqualTo("拳骨冲突");
    assertThat(profile("vista").skills()).hasSize(1);
  }

  @Test
  public void originsAndRecoveryDoNotInventFruitRequirementsOrHealing() {
    assertThat(skill("nami", "raitei").origin()).isEqualTo(Origin.EQUIPMENT);
    assertThat(skill("sanji", "exoskeleton").origin()).isEqualTo(Origin.RACIAL);
    assertThat(skill("king", "imperial-flame-dragon").origin()).isEqualTo(Origin.RACIAL);
    assertThat(skill("queen", "cyborg-laser").origin()).isEqualTo(Origin.EQUIPMENT);
    assertThat(skill("sabo", "dragon-claw").origin()).isEqualTo(Origin.TECHNIQUE);
    assertThat(skill("sabo", "fire-fist").requiredFruits()).contains("烧烧果实");
    assertThat(skill("blackbeard", "quake-strike").requiredFruits())
        .contains("震震果实")
        .doesNotContain("暗暗果实");
    assertThat(skill("kaido", "blast-breath").requiredFruits()).contains("鱼鱼果实青龙形态");
    assertThat(profile("chopper").skills())
        .allSatisfy(s -> assertThat(s.effects()).noneMatch(e -> e.type() == EffectType.HEAL));
    assertThat(profile("marco").skills())
        .anySatisfy(s -> assertThat(s.effects()).anyMatch(e -> e.type() == EffectType.HEAL));
    assertThat(profile("marco").skills())
        .allSatisfy(s -> assertThat(s.effects()).noneMatch(e -> e.type() == EffectType.POISON));
  }

  @Test
  public void lightningAndCharmExceptionsDoNotImmunizeAgainstPhysicalAttacks() {
    var luffy = profile("luffy");
    var rubber =
        FruitEntityBuilder.newBuilder()
            .setId(UUID.randomUUID())
            .setEnergyConsume(2)
            .setName(new I18nText().zhCN("动物系·人人果实·幻兽种·尼卡形态"))
            .build();
    var actor = CombatEngineTests.actor(CombatEngineTests.A, luffy, List.of(rubber));
    assertThat(actor.immuneTo(skill("enel", "el-thor"))).isTrue();
    assertThat(actor.immuneTo(skill("nami", "raitei"))).isTrue();
    assertThat(actor.immuneTo(skill("bigmom", "tenman-tenjin"))).isTrue();
    assertThat(actor.immuneTo(skill("kaido", "thunder-bagua"))).isFalse();
    assertThat(actor.immuneTo(skill("bigmom", "ikoku"))).isFalse();
    assertThat(actor.immuneTo(skill("hancock", "love-love-mellow"))).isTrue();
    assertThat(actor.immuneTo(skill("hancock", "perfume-femur"))).isFalse();
    var noFruit = CombatEngineTests.actor(CombatEngineTests.A, luffy, List.of());
    assertThat(noFruit.immuneTo(skill("enel", "el-thor"))).isFalse();
    assertThat(noFruit.immuneTo(skill("hancock", "love-love-mellow"))).isTrue();
  }

  @Test
  public void renamedOrNewMovesHaveIndependentIdsAndChangedRetainedSkillsAdvanceVersion()
      throws Exception {
    try (var stream = getClass().getResourceAsStream("/battle/catalog-v1.json")) {
      var previous = new ObjectMapper().readValue(stream, Catalog.class);
      for (var entry : previous.characters()) {
        var old = new HashMap<String, Skill>();
        entry.profile().skills().forEach(s -> old.put(s.id(), s));
        for (var next : profile(entry.code()).skills()) {
          var before = old.get(next.id());
          assertThat(next.version())
              .isEqualTo(
                  before == null
                      ? 1
                      : before.equals(next) ? before.version() : before.version() + 1);
        }
      }
    }
  }

  @Test
  public void fruitMovesRecognizeExistingDatabaseSeedNamesWithoutGuessingOtherModels() {
    var seeds =
        Map.ofEntries(
            Map.entry("luffy", "人人果实太阳神尼卡形态"),
            Map.entry("chopper", "人人果实"),
            Map.entry("robin", "花花果实"),
            Map.entry("brook", "黄泉果实"),
            Map.entry("whitebeard", "震动果实"),
            Map.entry("bigmom", "灵魂果实"),
            Map.entry("blackbeard", "黑暗果实"),
            Map.entry("akainu", "岩浆果实"),
            Map.entry("kuzan", "冷冷果实"),
            Map.entry("kizaru", "闪光果实"),
            Map.entry("fujitora", "重力果实"),
            Map.entry("law", "手术果实"),
            Map.entry("kid", "磁力果实"),
            Map.entry("kaido", "鱼鱼果实青龙型态"),
            Map.entry("yamato", "犬犬果实大口真神形态"),
            Map.entry("ace", "火焰果实"),
            Map.entry("sabo", "火焰果实"),
            Map.entry("marco", "鸟鸟果实不死鸟型态"),
            Map.entry("jozu", "闪亮果实"),
            Map.entry("katakuri", "糯米丸果实"),
            Map.entry("cracker", "饼干果实"),
            Map.entry("king", "龙龙果实无齿翼龙形态"),
            Map.entry("queen", "龙龙果实腕龙形态"),
            Map.entry("jack", "象象果实长毛象型态"),
            Map.entry("doflamingo", "线线果实"),
            Map.entry("crocodile", "沙砾果实"),
            Map.entry("enel", "轰雷果实"),
            Map.entry("lucci", "猫猫果实豹形态"),
            Map.entry("hancock", "迷恋果实"),
            Map.entry("kuma", "肉球果实"),
            Map.entry("moria", "影子果实"),
            Map.entry("perona", "鬼魂果实"),
            Map.entry("magellan", "毒毒果实"),
            Map.entry("bartolomeo", "护罩果实"),
            Map.entry("bonclay", "模仿果实"));
    for (var entry : seeds.entrySet()) {
      var fruits = new ArrayList<FruitEntity>();
      for (var name :
          entry.getKey().equals("blackbeard")
              ? List.of(entry.getValue(), "震动果实")
              : List.of(entry.getValue())) fruits.add(namedFruit(name));
      var actor = CombatEngineTests.actor(CombatEngineTests.A, profile(entry.getKey()), fruits);
      actor.loseBlood(1500);
      for (var skill : profile(entry.getKey()).skills())
        if (skill.origin() == Origin.FRUIT)
          assertThat(actor.canUse(skill)).as(entry.getKey() + " / " + skill.id()).isTrue();
    }
    var luffy =
        CombatEngineTests.actor(
            CombatEngineTests.A, profile("luffy"), List.of(namedFruit("人人果实太阳神尼卡形态")));
    assertThat(luffy.immuneTo(skill("enel", "el-thor"))).isTrue();
    var normalHumanFruit =
        CombatEngineTests.actor(CombatEngineTests.A, profile("luffy"), List.of(namedFruit("人人果实")));
    assertThat(normalHumanFruit.canUse(skill("luffy", "gear-five"))).isFalse();
    assertThat(normalHumanFruit.immuneTo(skill("enel", "el-thor"))).isFalse();
    var wrongModel =
        CombatEngineTests.actor(
            CombatEngineTests.A, profile("lucci"), List.of(namedFruit("猫猫果实剑齿虎形态")));
    assertThat(wrongModel.canUse(skill("lucci", "awakened-leopard"))).isFalse();
  }

  private FruitEntity namedFruit(String name) {
    return FruitEntityBuilder.newBuilder()
        .setId(UUID.randomUUID())
        .setEnergyConsume(2)
        .setName(new I18nText().zhCN(name))
        .build();
  }
}
