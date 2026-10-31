package com.yichi.onepiece.battle.core;

import static com.yichi.onepiece.battle.skill.CombatModels.*;

import com.yichi.onepiece.battle.model.CurrentCharacterEntityBuilder;
import com.yichi.onepiece.battle.model.CurrentCharacterEntityImpl;
import com.yichi.onepiece.battle.skill.CombatCatalog;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.fruit.model.FruitEntity;
import com.yichi.onepiece.model.Haki;
import com.yichi.onepiece.model.I18nText;
import java.util.*;

/**
 * A per-battle actor. Skill definitions never change; cooldowns/statuses are local to this actor.
 */
public class CharacterInBattle {
  private static final int BASE_COST = 2;
  private final CharacterEntity base;
  private final List<FruitEntity> fruits;
  private final Profile profile;
  private final Set<Haki> hakis;
  private final Map<String, Integer> readyAt = new HashMap<>(), uses = new HashMap<>();
  private final Map<EffectType, Status> statuses = new EnumMap<>(EffectType.class);
  private CurrentCharacterEntityImpl current;
  private int opportunities, shield, controlImmunity;
  private boolean reversed;
  private int resultScore;

  private record Status(double power, int expires, Origin origin, UUID source) {}

  public CharacterInBattle(CharacterEntity base, List<FruitEntity> fruits) {
    this(base, fruits, CombatCatalog.basic(), List.of());
  }

  public CharacterInBattle(
      CharacterEntity base, List<FruitEntity> fruits, Profile profile, List<Haki> hakis) {
    this.base = base;
    this.resultScore = base.getScore();
    this.fruits = List.copyOf(fruits);
    this.profile = profile;
    this.hakis = Set.copyOf(hakis);
    current =
        (CurrentCharacterEntityImpl)
            CurrentCharacterEntityBuilder.newBuilder()
                .setCharacterId(base.getId())
                .setAttack(base.getAttack())
                .setBlood(base.getBlood())
                .setDefense(base.getDefense())
                .setIntelligence(base.getIntelligence())
                .setCurrentEnergy(base.getEnergy())
                .setCurrentEnergyConsume(
                    BASE_COST + fruits.stream().mapToInt(FruitEntity::getEnergyConsume).sum())
                .setSpeed(base.getSpeed())
                .build();
  }

  public UUID getCharacterId() {
    return base.getId();
  }

  public I18nText getName() {
    return base.getName();
  }

  public int getScore() {
    return resultScore;
  }

  public void setResultScore(int score) {
    this.resultScore = score;
  }

  public boolean getReversed() {
    return reversed;
  }

  public boolean isAlive() {
    return current.getBlood() > 0 && current.getCurrentEnergy() > 0;
  }

  public int getBlood() {
    return current.getBlood();
  }

  public int getSpeed() {
    return current.getSpeed();
  }

  public int getIntelligence() {
    return current.getIntelligence();
  }

  public int getEnergyConsume() {
    return current.getCurrentEnergyConsume();
  }

  public int energy() {
    return current.getCurrentEnergy();
  }

  public int maxBlood() {
    return base.getBlood();
  }

  public Profile profile() {
    return profile;
  }

  public boolean hasHaki(Haki haki) {
    return hakis.contains(haki);
  }

  public double status(EffectType type) {
    var s = statuses.get(type);
    return s != null && s.expires() > opportunities ? s.power() : 0;
  }

  public boolean coating() {
    var s = statuses.get(EffectType.COATING);
    return s != null
        && s.expires() > opportunities
        && profile.coatingLevel() > 0
        && hasHaki(Haki.HAOSHOKU);
  }

  public double evasion() {
    return status(EffectType.EVASION) + (hasHaki(Haki.KENBUNSHOKU) ? .04 : 0);
  }

  public int attackPower() {
    return Math.max(
        0,
        (int)
            Math.round(
                current.getAttack()
                    * (1 + status(EffectType.ATTACK_UP) - status(EffectType.ATTACK_DOWN))));
  }

  public int defensePower() {
    return Math.max(
        0, (int) Math.round(current.getDefense() * (1 - status(EffectType.DEFENSE_DOWN))));
  }

  public CurrentCharacterEntityImpl getCurrentCharacterEntity() {
    // Snapshot builders receive an independent value, never the mutable actor instance.
    return (CurrentCharacterEntityImpl)
        CurrentCharacterEntityBuilder.newBuilder()
            .setCharacterId(getCharacterId())
            .setAttack(attackPower())
            .setDefense(defensePower())
            .setSpeed(getSpeed())
            .setIntelligence(getIntelligence())
            .setBlood(getBlood())
            .setCurrentEnergy(energy())
            .setCurrentEnergyConsume(getEnergyConsume())
            .build();
  }

  public List<Outcome> beginAction() {
    opportunities++;
    if (controlImmunity > 0) controlImmunity--;
    statuses.entrySet().removeIf(e -> e.getValue().expires() <= opportunities);
    reverseIfNeeded();
    var effects = new ArrayList<Outcome>();
    double poison = status(EffectType.POISON);
    if (poison > 0) {
      int damage = loseBlood((int) Math.ceil(maxBlood() * poison));
      effects.add(new Outcome("POISON", getCharacterId().toString(), damage, 0));
    }
    return effects;
  }

  public boolean consumeControl() {
    var control = statuses.remove(EffectType.STUN);
    if (control == null) return false;
    controlImmunity = 3;
    return true;
  }

  public List<Skill> legalSkills() {
    return profile.skills().stream().filter(this::canUse).toList();
  }

  public boolean immuneTo(Skill skill) {
    return skill != null && !Collections.disjoint(profile.immunities(), skill.tags());
  }

  public boolean canUse(Skill skill) {
    if (!isAlive()
        || !profile.skills().contains(skill)
        || energy() <= cost(skill)
        || opportunities < readyAt.getOrDefault(skill.id(), 0)
        || uses.getOrDefault(skill.id(), 0) >= skill.maxUses()
        || (double) getBlood() / maxBlood() > skill.healthBelow()) return false;
    if (skill.origin() == Origin.FRUIT && (reversed || !hasRequiredFruit(skill))) return false;
    if (skill.effects().stream().anyMatch(e -> e.type() == EffectType.COATING)
        && (profile.coatingLevel() == 0 || !hasHaki(Haki.HAOSHOKU) || coating())) return false;
    if (skill.effects().stream().anyMatch(e -> e.type() == EffectType.HEAL)
        && getBlood() >= maxBlood() * .80) return false;
    if (skill.effects().stream().anyMatch(e -> e.type() == EffectType.ENERGY)
        && energy() >= base.getEnergy() * .65) return false;
    return true;
  }

  private boolean hasRequiredFruit(Skill skill) {
    for (var fruit : fruits)
      for (String required : skill.requiredFruits()) {
        var name = fruit.getName();
        if (name == null) continue;
        if (fruitName(required).equals(fruitName(name.getZhCN()))
            || fruitName(required).equals(fruitName(name.getEnUS()))) return true;
      }
    return false;
  }

  private static String fruitName(String name) {
    return CombatCatalog.normalize(name)
        .replaceAll("动物系|超人系|自然系|幻兽种|古代种|mythicalzoan|ancientzoan|model|形态|型态", "")
        .replaceAll("[^\\p{L}\\p{N}]", "");
  }

  public int cost(Skill skill) {
    return getEnergyConsume() + (skill == null ? 0 : skill.energy()) + (coating() ? 4 : 0);
  }

  public int spendAction(Skill skill) {
    int spent = Math.min(energy(), cost(skill));
    current.setCurrentEnergy(Math.max(0, energy() - spent));
    if (skill != null) {
      readyAt.put(skill.id(), opportunities + skill.cooldown() + 1);
      uses.merge(skill.id(), 1, Integer::sum);
    }
    return spent;
  }

  public int receive(double attack, double piercing) {
    int defense = defensePower();
    if (hasHaki(Haki.BUSOSHOKU)) defense = (int) Math.round(defense * 1.04);
    double reduction =
        Math.min(.60, status(EffectType.GUARD) + (coating() ? .06 * profile.coatingLevel() : 0));
    int damage =
        Math.max(
            0,
            (int) Math.round((attack - defense * (1 - Math.min(.35, piercing))) * (1 - reduction)));
    int blocked = Math.min(shield, damage);
    shield -= blocked;
    return loseBlood(damage - blocked);
  }

  public int loseBlood(int damage) {
    int actual = Math.min(getBlood(), Math.max(0, damage));
    current.setBlood(getBlood() - actual);
    return actual;
  }

  public int spendDefense() {
    int cost = Math.min(2, energy());
    current.setCurrentEnergy(Math.max(0, energy() - cost));
    return cost;
  }

  public Outcome apply(Effect effect, Origin origin) {
    return apply(effect, origin, getCharacterId());
  }

  public Outcome apply(Effect effect, Origin origin, UUID source) {
    int amount = 0;
    switch (effect.type()) {
      case HEAL -> {
        amount = Math.min(maxBlood() - getBlood(), (int) Math.round(maxBlood() * effect.power()));
        current.setBlood(getBlood() + amount);
      }
      case SHIELD -> {
        int next =
            Math.min(
                (int) Math.round(maxBlood() * .30),
                Math.max(shield, (int) Math.round(maxBlood() * effect.power())));
        amount = next - shield;
        shield = next;
      }
      case ENERGY -> {
        amount =
            Math.min(
                base.getEnergy() - energy(),
                (int) Math.round(base.getEnergy() * Math.min(.15, effect.power())));
        current.setCurrentEnergy(energy() + amount);
      }
      case STUN -> {
        if (controlImmunity == 0 && !statuses.containsKey(EffectType.STUN)) {
          statuses.put(EffectType.STUN, new Status(0, opportunities + 2, origin, source));
          amount = 1;
        }
      }
      case DAMAGE -> throw new IllegalArgumentException("Damage needs a hit resolution");
      case COATING -> {
        statuses.put(
            EffectType.COATING,
            new Status(0, opportunities + effect.duration() + 1, origin, source));
        amount = profile.coatingLevel();
      }
      default -> {
        statuses.put(
            effect.type(),
            new Status(effect.power(), opportunities + effect.duration() + 1, origin, source));
        amount = (int) Math.round(effect.power() * 10000);
      }
    }
    String unit =
        switch (effect.type()) {
          case GUARD, EVASION, ATTACK_UP, ATTACK_DOWN, DEFENSE_DOWN, POISON -> "BASIS_POINTS";
          case STUN -> "ACTIONS";
          case COATING -> "LEVEL";
          case ENERGY -> "ENERGY";
          default -> "HP";
        };
    return new Outcome(
        effect.type().name(), getCharacterId().toString(), amount, effect.duration(), unit);
  }

  public void maintainCoating() {
    if (coating() && energy() <= 8) statuses.remove(EffectType.COATING);
  }

  private void reverseIfNeeded() {
    if (reversed || fruits.isEmpty() || energy() >= base.getEnergy() / 10) return;
    current.setAttack(base.getBaseAttack());
    current.setDefense(base.getBaseDefense());
    current.setSpeed(base.getBaseSpeed());
    current.setIntelligence(base.getBaseIntelligence());
    current.setCurrentEnergyConsume(BASE_COST);
    statuses
        .entrySet()
        .removeIf(
            e ->
                e.getValue().origin() == Origin.FRUIT
                    && getCharacterId().equals(e.getValue().source()));
    reversed = true;
  }

  public AttackEntity attack() {
    reverseIfNeeded();
    spendAction(null);
    return AttackEntityBuilder.newBuilder().setAttack(attackPower()).build();
  }

  public List<Integer> defend(AttackEntity attack) {
    return List.of(receive(attack.getAttack(), 0), spendDefense());
  }
}
