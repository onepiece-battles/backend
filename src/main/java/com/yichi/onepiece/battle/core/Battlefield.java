package com.yichi.onepiece.battle.core;

import static com.yichi.onepiece.battle.skill.CombatModels.*;

import com.yichi.onepiece.battle.model.*;
import com.yichi.onepiece.model.Haki;
import java.util.*;

/** Seeded, bounded rule execution. AI supplies only optional skill preferences. */
public class Battlefield {
  private final UUID battleId;
  private final CharacterInBattle starter, receiver;
  private final SplittableRandom random;
  private final String locale;
  private final Tactics tactics;
  private final List<BattleStatusDetailEntity> statuses = new ArrayList<>();
  private final List<BattleProcessDetailEntity> processes = new ArrayList<>();
  private final List<Event> events = new ArrayList<>();
  private CharacterInBattle attacker, defender;

  public Battlefield(CharacterInBattle starter, CharacterInBattle receiver, UUID battleId) {
    this(
        starter,
        receiver,
        battleId,
        battleId.getMostSignificantBits() ^ battleId.getLeastSignificantBits(),
        "zh_CN",
        Tactics.rules());
  }

  public Battlefield(
      CharacterInBattle starter,
      CharacterInBattle receiver,
      UUID battleId,
      long seed,
      String locale,
      Tactics tactics) {
    this.starter = starter;
    this.receiver = receiver;
    this.battleId = battleId;
    this.random = new SplittableRandom(seed);
    this.locale = locale;
    this.tactics = tactics;
    attacker = starter;
    defender = receiver;
    refreshAttackSide();
  }

  public boolean allAlive() {
    return starter.isAlive() && receiver.isAlive();
  }

  public boolean mutualDefeat() {
    return !starter.isAlive() && !receiver.isAlive();
  }

  public CharacterInBattle getWinner() {
    return starter.isAlive() ? starter : receiver;
  }

  public CharacterInBattle getLoser() {
    return starter.isAlive() ? receiver : starter;
  }

  public CharacterInBattle getAttackSide() {
    return attacker;
  }

  public CharacterInBattle getDefenseSide() {
    return defender;
  }

  public List<BattleStatusDetailEntity> getBattleStatusDetailEntities() {
    return statuses;
  }

  public List<BattleProcessDetailEntity> getBattleProcessDetailEntities() {
    return processes;
  }

  public List<Event> events() {
    return List.copyOf(events);
  }

  public void completeFinalStatus() {
    snapshot();
  }

  public void checkReverse() {
    /* Fruit reversion is checked at the actor's action boundary. */
  }

  private void snapshot() {
    statuses.add(
        BattleStatusDetailEntityBuilder.newBuilder()
            .setBattleId(battleId)
            .setBattleStatusRoundNumber(statuses.size() + 1)
            .setInitSide(starter.getCurrentCharacterEntity())
            .setReceiveSide(receiver.getCurrentCharacterEntity())
            .build());
  }

  public double dodgeChance(CharacterInBattle source, CharacterInBattle target) {
    double delta =
        (target.getSpeed() + .5 * target.getIntelligence())
            - (source.getSpeed() + .5 * source.getIntelligence());
    return Math.max(.05, Math.min(.75, .22 + delta / 600 + target.evasion()));
  }

  public void refreshAttackSide() {
    double first = initiative(starter), second = initiative(receiver);
    attacker = random.nextDouble() * (first + second) < first ? starter : receiver;
    defender = attacker == starter ? receiver : starter;
  }

  private static double initiative(CharacterInBattle actor) {
    return Math.max(1, actor.getIntelligence() + .25 * actor.getSpeed());
  }

  private Skill choose() {
    var legal = attacker.legalSkills();
    double basic =
        Math.max(0, attacker.attackPower() - defender.defensePower())
            / Math.max(1.0, defender.maxBlood())
            * 100;
    return legal.stream()
        .max(Comparator.comparingDouble(s -> utility(s, attacker, defender)))
        .filter(s -> utility(s, attacker, defender) > basic)
        .orElse(null);
  }

  private double utility(Skill skill, CharacterInBattle self, CharacterInBattle enemy) {
    if (skill.target() != Target.SELF && enemy.immuneTo(skill)) return -Double.MAX_VALUE;
    double score = 0;
    for (var effect : skill.effects())
      score +=
          switch (effect.type()) {
            case DAMAGE ->
                Math.max(0, self.attackPower() * effect.power() - enemy.defensePower())
                    / Math.max(1.0, enemy.maxBlood())
                    * 100;
            case HEAL -> (1 - (double) self.getBlood() / self.maxBlood()) * 60;
            case SHIELD, GUARD ->
                (self.getBlood() < self.maxBlood() * .65 ? 10 : 1)
                    * (self.status(EffectType.GUARD) > 0 ? .1 : 1);
            case EVASION -> self.status(EffectType.EVASION) > 0 ? 0 : 8;
            case ATTACK_UP -> self.status(EffectType.ATTACK_UP) > 0 ? 0 : 14;
            case ATTACK_DOWN -> enemy.status(EffectType.ATTACK_DOWN) > 0 ? 0 : 12;
            case DEFENSE_DOWN -> enemy.status(EffectType.DEFENSE_DOWN) > 0 ? 0 : 12;
            case POISON -> enemy.status(EffectType.POISON) > 0 ? 0 : 11;
            case STUN -> 8;
            case ENERGY -> 18;
            case COATING -> 22;
          };
    score -= skill.energy() * .2;
    var order = tactics.priorities().getOrDefault(self.getCharacterId().toString(), List.of());
    int rank = order.indexOf(skill.id());
    if (rank >= 0) score += Math.min(5, (order.size() - rank) * 1.5);
    return score;
  }

  public void processBattle() {
    snapshot();
    int round = processes.size() + 1;
    var outcomes = new ArrayList<>(attacker.beginAction());
    attacker.maintainCoating();
    if (!attacker.isAlive()) {
      record(
          round,
          null,
          false,
          0,
          0,
          0,
          outcomes,
          english() ? "Defeated by damage over time" : "持续伤害耗尽");
      return;
    }
    if (attacker.consumeControl()) {
      outcomes.add(
          new Outcome("CONTROL_SKIP", attacker.getCharacterId().toString(), 1, 0, "ACTIONS"));
      record(round, null, false, 0, 0, 0, outcomes, english() ? "Action interrupted" : "行动受阻");
      return;
    }
    Skill skill = choose();
    int spent = attacker.spendAction(skill), damage = 0, defenseCost = 0;
    boolean missed = false;
    var effects = skill == null ? List.of(new Effect(EffectType.DAMAGE, 1, 0)) : skill.effects();
    boolean hostile = skill == null || skill.target() != Target.SELF;
    if (hostile) missed = random.nextDouble() < dodgeChance(attacker, defender);
    if (hostile && defender.immuneTo(skill))
      outcomes.add(new Outcome("IMMUNE", defender.getCharacterId().toString(), 0, 0, "NONE"));
    else if (!missed) {
      double coating = attacker.coating() ? .08 * attacker.profile().coatingLevel() : 0;
      double piercing = attacker.coating() ? .05 * attacker.profile().coatingLevel() : 0;
      for (var effect : effects) {
        if (effect.type() == EffectType.DAMAGE) {
          double raw =
              attacker.attackPower()
                  * effect.power()
                  * (1 + coating)
                  * (attacker.hasHaki(Haki.BUSOSHOKU) ? 1.04 : 1);
          int amount = defender.receive(raw, piercing);
          damage += amount;
          outcomes.add(new Outcome("DAMAGE", defender.getCharacterId().toString(), amount, 0));
        } else
          outcomes.add(
              (hostile ? defender : attacker)
                  .apply(effect, skill.origin(), attacker.getCharacterId()));
      }
      if (hostile) defenseCost = defender.spendDefense();
    }
    String label = skill == null ? (english() ? "Basic attack" : "普通攻击") : skill.label(locale);
    if (attacker.coating() && hostile) label += (english() ? " · Conqueror coating" : " · 霸缠");
    StringBuilder summary = new StringBuilder(label);
    for (var effect : outcomes)
      if (effect.type().equals("HEAL"))
        summary.append(english() ? " · HP +" : " · 回复 ").append(effect.amount());
    record(round, skill, missed, spent, damage, defenseCost, outcomes, summary.toString());
  }

  private boolean english() {
    return locale != null && locale.startsWith("en");
  }

  private void record(
      int round,
      Skill skill,
      boolean missed,
      int spent,
      int damage,
      int defenseCost,
      List<Outcome> outcomes,
      String label) {
    processes.add(
        BattleProcessDetailEntityBuilder.newBuilder()
            .setBattleId(battleId)
            .setBattleProcessRoundNumber(round)
            .setAttackerId(attacker.getCharacterId())
            .setAttackSkill(label)
            .setAttackMissed(missed)
            .setAttackerEnergyLoss(spent)
            .setDefenderBloodLoss(damage)
            .setDefenderEnergyLoss(defenseCost)
            .build());
    events.add(
        new Event(
            round,
            attacker.getCharacterId().toString(),
            skill == null ? "basic" : skill.id(),
            skill == null ? 1 : skill.version(),
            label,
            missed,
            spent,
            outcomes));
  }
}
