package com.yichi.onepiece.battle.skill;

import static com.yichi.onepiece.battle.skill.CombatModels.*;
import static org.assertj.core.api.Assertions.*;

import com.yichi.onepiece.battle.core.*;
import com.yichi.onepiece.character.model.entity.*;
import com.yichi.onepiece.fruit.model.*;
import com.yichi.onepiece.model.*;
import java.util.*;
import org.testng.annotations.Test;

public class CombatEngineTests {
  static final Evidence SOURCE =
      new Evidence("https://one-piece.com/character/", "Test fixture; game balance values");
  static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001"),
      B = UUID.fromString("00000000-0000-0000-0000-000000000002");

  static CharacterEntity character(UUID id, String name) {
    return CharacterEntityBuilder.newBuilder()
        .setId(id)
        .setName(new I18nText().zhCN(name).enUS(name))
        .setSystemResource(true)
        .setScore(2000)
        .setBlood(4000)
        .setBaseBlood(4000)
        .setAttack(800)
        .setBaseAttack(600)
        .setDefense(500)
        .setBaseDefense(450)
        .setSpeed(120)
        .setBaseSpeed(100)
        .setIntelligence(150)
        .setBaseIntelligence(140)
        .setEnergy(250)
        .build();
  }

  static Skill skill(
      String id,
      Origin origin,
      Target target,
      int energy,
      int cooldown,
      int uses,
      double health,
      Effect... effects) {
    return new Skill(
        id,
        1,
        id,
        id,
        origin,
        target,
        energy,
        cooldown,
        uses,
        health,
        origin == Origin.FRUIT ? List.of("测试果实") : List.of(),
        List.of(effects),
        List.of(SOURCE));
  }

  static Profile profile(int coating, Skill... skills) {
    return new Profile(1, "fixture", "test", coating, List.of(skills), List.of(SOURCE));
  }

  static CharacterInBattle actor(
      UUID id, Profile profile, List<FruitEntity> fruits, Haki... hakis) {
    return new CharacterInBattle(character(id, id.toString()), fruits, profile, List.of(hakis));
  }

  static FruitEntity fruit() {
    return FruitEntityBuilder.newBuilder()
        .setId(UUID.randomUUID())
        .setName(new I18nText().zhCN("测试果实"))
        .setEnergyConsume(2)
        .build();
  }

  @Test
  public void catalogueHasFiftyIdentitiesAndBoundedSourceBackedSkills() {
    var catalog = new CombatCatalog();
    assertThat(catalog.catalog().characters()).hasSize(50);
    assertThat(
            catalog.catalog().characters().stream()
                .mapToInt(c -> c.profile().skills().size())
                .sum())
        .isEqualTo(115);
    assertThat(catalog.find(new I18nText().zhCN("假路飞").enUS("Fake Luffy"))).isEmpty();
    assertThat(catalog.find(new I18nText().zhCN("凯多").enUS("Shanks"))).isEmpty();
    assertThat(catalog.find(new I18nText().zhCN("路飞")).orElseThrow().profile().coatingLevel())
        .isPositive();
    for (var entry : catalog.catalog().characters())
      for (var s : entry.profile().skills()) {
        assertThat(s.evidence()).isNotEmpty();
        if (s.origin() == Origin.FRUIT) assertThat(s.requiredFruits()).isNotEmpty();
        assertThat(s.cooldown()).isBetween(0, 8);
        assertThat(s.maxUses()).isBetween(1, 12);
      }
  }

  @Test
  public void dodgeUsesActualDefenderAndDoesNotTruncateSmallDifferences() {
    var slow =
        new CharacterInBattle(character(A, "slow").toBuilder().setSpeed(100).build(), List.of());
    var fast =
        new CharacterInBattle(character(B, "fast").toBuilder().setSpeed(110).build(), List.of());
    var field = new Battlefield(slow, fast, UUID.randomUUID());
    assertThat(field.dodgeChance(slow, fast)).isGreaterThan(field.dodgeChance(fast, slow));
    assertThat(field.dodgeChance(slow, fast) + field.dodgeChance(fast, slow))
        .isCloseTo(.44, within(.00001));
    assertThat(field.dodgeChance(slow, fast)).isBetween(.05, .75);
  }

  @Test
  public void skillCooldownAndUsesResetForEachActorButDefinitionsStayImmutable() {
    var hit =
        skill(
            "test.hit",
            Origin.TECHNIQUE,
            Target.ENEMY,
            8,
            2,
            2,
            1,
            new Effect(EffectType.DAMAGE, 1.4, 0));
    var a = actor(A, profile(0, hit), List.of());
    a.beginAction();
    assertThat(a.canUse(hit)).isTrue();
    a.spendAction(hit);
    a.beginAction();
    assertThat(a.canUse(hit)).isFalse();
    a.beginAction();
    assertThat(a.canUse(hit)).isFalse();
    a.beginAction();
    assertThat(a.canUse(hit)).isTrue();
    a.spendAction(hit);
    for (int i = 0; i < 4; i++) a.beginAction();
    assertThat(a.canUse(hit)).isFalse();
    var next = actor(A, profile(0, hit), List.of());
    next.beginAction();
    assertThat(next.canUse(hit)).isTrue();
    assertThat(hit.maxUses()).isEqualTo(2);
  }

  @Test
  public void recoveryAndShieldsAreCappedAndActionsCannotOverdrawEnergy() {
    var heal =
        skill(
            "test.heal",
            Origin.TECHNIQUE,
            Target.SELF,
            14,
            3,
            2,
            .7,
            new Effect(EffectType.HEAL, .25, 0));
    var a = actor(A, profile(0, heal), List.of());
    assertThat(a.canUse(heal)).isFalse();
    a.loseBlood(2000);
    assertThat(a.canUse(heal)).isTrue();
    assertThat(a.apply(heal.effects().get(0), heal.origin()).amount()).isEqualTo(1000);
    assertThat(a.getBlood()).isEqualTo(3000);
    a.apply(heal.effects().get(0), heal.origin());
    a.apply(heal.effects().get(0), heal.origin());
    assertThat(a.getBlood()).isEqualTo(a.maxBlood());
    var shield = new Effect(EffectType.SHIELD, .25, 0);
    assertThat(a.apply(shield, Origin.TECHNIQUE).amount()).isEqualTo(1000);
    assertThat(a.apply(shield, Origin.TECHNIQUE).amount()).isZero();
    var weak =
        new CharacterInBattle(
            character(A, "low").toBuilder().setEnergy(16).build(),
            List.of(),
            profile(0, heal),
            List.of());
    weak.loseBlood(2000);
    assertThat(weak.canUse(heal)).isFalse();
  }

  @Test
  public void controlHasAnImmunityWindowAndPoisonCannotKillBelowZero() {
    var a = actor(A, profile(0), List.of());
    assertThat(a.apply(new Effect(EffectType.STUN, 0, 1), Origin.FRUIT, B).amount()).isEqualTo(1);
    a.beginAction();
    assertThat(a.consumeControl()).isTrue();
    assertThat(a.apply(new Effect(EffectType.STUN, 0, 1), Origin.FRUIT, B).amount()).isZero();
    a.beginAction();
    assertThat(a.consumeControl()).isFalse();
    a.beginAction();
    a.beginAction();
    assertThat(a.apply(new Effect(EffectType.STUN, 0, 1), Origin.FRUIT, B).amount()).isEqualTo(1);
    a.loseBlood(3999);
    a.apply(new Effect(EffectType.POISON, .025, 3), Origin.FRUIT, B);
    a.beginAction();
    assertThat(a.getBlood()).isZero();
  }

  @Test
  public void fruitReversionDoesNotEraseCoatingOrOpponentInflictedPoison() {
    var a = actor(A, profile(2), List.of(fruit()), Haki.HAOSHOKU);
    a.apply(new Effect(EffectType.COATING, 0, 3), Origin.HAKI);
    a.apply(new Effect(EffectType.ATTACK_UP, .20, 3), Origin.FRUIT);
    a.apply(new Effect(EffectType.POISON, .025, 3), Origin.FRUIT, B);
    while (a.energy() > 20) a.spendAction(null);
    a.beginAction();
    assertThat(a.getReversed()).isTrue();
    assertThat(a.status(EffectType.ATTACK_UP)).isZero();
    assertThat(a.coating()).isTrue();
    assertThat(a.status(EffectType.POISON)).isPositive();
    assertThat(a.getCurrentCharacterEntity().getAttack()).isEqualTo(600);
  }

  @Test
  public void fruitSkillsRequireTheActualHeldFruitAndOrdinaryConquerorsDoNotGetCoating() {
    var power =
        skill(
            "test.fruit",
            Origin.FRUIT,
            Target.ENEMY,
            8,
            1,
            3,
            1,
            new Effect(EffectType.DAMAGE, 1.4, 0));
    assertThat(actor(A, profile(0, power), List.of()).canUse(power)).isFalse();
    assertThat(actor(A, profile(0, power), List.of(fruit())).canUse(power)).isTrue();
    var coating =
        skill(
            "test.coating",
            Origin.HAKI,
            Target.SELF,
            12,
            4,
            3,
            1,
            new Effect(EffectType.COATING, 0, 3));
    assertThat(actor(A, profile(0, coating), List.of(), Haki.HAOSHOKU).canUse(coating)).isFalse();
    assertThat(actor(A, profile(2, coating), List.of()).canUse(coating)).isFalse();
    assertThat(actor(A, profile(2, coating), List.of(), Haki.HAOSHOKU).canUse(coating)).isTrue();
  }

  private Battlefield run(long seed) {
    var hit =
        skill(
            "test.hit",
            Origin.TECHNIQUE,
            Target.ENEMY,
            8,
            2,
            5,
            1,
            new Effect(EffectType.DAMAGE, 1.4, 0));
    var field =
        new Battlefield(
            actor(A, profile(0, hit), List.of()),
            actor(B, profile(0, hit), List.of()),
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            seed,
            "zh_CN",
            Tactics.rules());
    for (int i = 0; i < 120 && field.allAlive(); i++) {
      field.processBattle();
      if (field.allAlive()) field.refreshAttackSide();
    }
    field.completeFinalStatus();
    return field;
  }

  @Test
  public void fixedSeedAndFrozenProfilesReproduceEveryRecordedAction() {
    var first = run(123);
    var second = run(123);
    assertThat(first.events()).isEqualTo(second.events());
    assertThat(first.events()).isNotEqualTo(run(124).events());
    assertThat(first.getBattleStatusDetailEntities().get(0).getInitSide().getBlood())
        .isEqualTo(4000);
    assertThat(first.events()).anyMatch(e -> e.skillId().equals("test.hit"));
  }

  @Test
  public void coatingMakesHitsStrongerWithoutChangingBaseStats() {
    long normal = 0, strong = 0;
    for (int seed = 0; seed < 100; seed++) {
      var a = actor(A, profile(2), List.of(), Haki.HAOSHOKU);
      a.apply(new Effect(EffectType.COATING, 0, 3), Origin.HAKI);
      var coated =
          new Battlefield(
              a,
              actor(B, profile(0), List.of()),
              UUID.randomUUID(),
              seed,
              "zh_CN",
              Tactics.rules());
      coated.processBattle();
      var plain =
          new Battlefield(
              actor(A, profile(0), List.of()),
              actor(B, profile(0), List.of()),
              UUID.randomUUID(),
              seed,
              "zh_CN",
              Tactics.rules());
      plain.processBattle();
      if (coated.events().get(0).actor().equals(A.toString()))
        strong += coated.getBattleProcessDetailEntities().get(0).getDefenderBloodLoss();
      if (plain.events().get(0).actor().equals(A.toString()))
        normal += plain.getBattleProcessDetailEntities().get(0).getDefenderBloodLoss();
      assertThat(a.profile().version()).isEqualTo(1);
    }
    assertThat(strong).isGreaterThan((long) (normal * 1.2));
  }

  @Test
  public void unsafeEffectsAndEnemyHealingAreRejected() {
    assertThatThrownBy(() -> new Effect(EffectType.HEAL, .9, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Effect(EffectType.STUN, 0, 3))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                skill(
                    "test.self",
                    Origin.TECHNIQUE,
                    Target.SELF,
                    5,
                    1,
                    1,
                    1,
                    new Effect(EffectType.DAMAGE, 1, 0)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                skill(
                    "test.bad",
                    Origin.TECHNIQUE,
                    Target.ENEMY,
                    5,
                    1,
                    1,
                    1,
                    new Effect(EffectType.HEAL, .2, 0)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void usoppResistsOnlyNegativeHollowAndOtherGhostAttacksStillWork() {
    var catalog = new CombatCatalog();
    var usopp = catalog.find(new I18nText().zhCN("乌索普")).orElseThrow().profile();
    var perona = catalog.find(new I18nText().zhCN("佩罗娜")).orElseThrow().profile();
    var actor = actor(A, usopp, List.of());
    assertThat(actor.immuneTo(perona.skills().get(0))).isTrue();
    assertThat(actor.immuneTo(perona.skills().get(1))).isFalse();
  }
}
