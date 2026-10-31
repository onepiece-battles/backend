package com.yichi.onepiece.battle.skill;

import static com.yichi.onepiece.battle.skill.CombatModels.*;
import static org.assertj.core.api.Assertions.*;

import com.yichi.onepiece.battle.core.*;
import com.yichi.onepiece.fruit.model.*;
import com.yichi.onepiece.model.*;
import java.nio.file.*;
import java.util.*;
import org.testng.annotations.Test;

/** Equal baseline stats isolate mechanics; this is a smoke/bounds simulation, not canon ranking. */
public class CombatSimulationTests {
  @Test
  public void everyPresetRunsBoundedBattlesWithoutIllegalSkillsOrResourceOverflow()
      throws Exception {
    var entries = new CombatCatalog().catalog().characters();
    int games = 0, heals = 0, coatings = 0, misses = 0, draws = 0, totalTurns = 0;
    var wins = new int[2];
    for (int i = 0; i < entries.size(); i++)
      for (int seed = 0; seed < 3; seed++) {
        var a = entries.get(i).profile();
        var b = entries.get((i + 1 + seed * 7) % entries.size()).profile();
        for (int orientation = 0; orientation < 2; orientation++) {
          Profile first = orientation == 0 ? a : b, second = orientation == 0 ? b : a;
          var left = actor(CombatEngineTests.A, first);
          var right = actor(CombatEngineTests.B, second);
          var field =
              new Battlefield(
                  left, right, UUID.randomUUID(), i * 100 + seed, "zh_CN", Tactics.rules());
          int turns = 0;
          while (field.allAlive() && turns < 120) {
            field.processBattle();
            if (field.allAlive()) field.refreshAttackSide();
            turns++;
          }
          games++;
          totalTurns += turns;
          if (field.allAlive() || field.mutualDefeat()) draws++;
          else wins[field.getWinner() == left ? 0 : 1]++;
          for (var actor : List.of(left, right)) {
            assertThat(actor.getBlood()).isBetween(0, actor.maxBlood());
            assertThat(actor.energy()).isBetween(0, 250);
          }
          for (var event : field.events()) {
            if (event.missed()) misses++;
            for (var effect : event.effects()) {
              assertThat(effect.amount()).isNotNegative();
              if (effect.type().equals("HEAL")) heals++;
              if (effect.type().equals("COATING")) coatings++;
            }
            if (!event.skillId().equals("basic")) {
              var profile = event.actor().equals(left.getCharacterId().toString()) ? first : second;
              assertThat(profile.skills()).anyMatch(s -> s.id().equals(event.skillId()));
            }
          }
        }
      }
    assertThat(games).isEqualTo(300);
    assertThat(heals).isPositive();
    assertThat(coatings).isPositive();
    assertThat(misses).isPositive();
    Path report = Path.of("build/reports/combat-simulation.txt");
    Files.createDirectories(report.getParent());
    Files.writeString(
        report,
        "Equal-stat mechanics smoke simulation (not a tier list)\ngames="
            + games
            + "\nmean_actions="
            + (double) totalTurns / games
            + "\ndraws="
            + draws
            + "\nstarter_wins="
            + wins[0]
            + "\nreceiver_wins="
            + wins[1]
            + "\nheals="
            + heals
            + "\ncoating_activations="
            + coatings
            + "\nmisses="
            + misses
            + "\n");
  }

  private CharacterInBattle actor(UUID id, Profile p) {
    var names = new LinkedHashSet<String>();
    for (var skill : p.skills())
      if (skill.origin() == Origin.FRUIT && !skill.requiredFruits().isEmpty())
        names.add(skill.requiredFruits().get(0));
    var fruits = new ArrayList<FruitEntity>();
    for (String name : names)
      fruits.add(
          FruitEntityBuilder.newBuilder()
              .setId(UUID.randomUUID())
              .setName(new I18nText().zhCN(name))
              .setEnergyConsume(2)
              .build());
    return new CharacterInBattle(
        CombatEngineTests.character(id, p.identity()),
        fruits,
        p,
        p.coatingLevel() > 0
            ? List.of(Haki.HAOSHOKU, Haki.BUSOSHOKU, Haki.KENBUNSHOKU)
            : List.of(Haki.BUSOSHOKU, Haki.KENBUNSHOKU));
  }
}
