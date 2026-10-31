package com.yichi.onepiece.battle.skill;

import java.util.*;

/** Immutable, versioned data. Effects are engine primitives; executable code is never a skill. */
public final class CombatModels {
  private CombatModels() {}

  public static final String ENGINE_VERSION = "skills-v2";
  private static final Set<String> INTERACTION_TAGS =
      Set.of("NEGATIVE_HOLLOW", "PURE_LIGHTNING", "HANCOCK_MELLOW");

  public enum Origin {
    TECHNIQUE,
    FRUIT,
    HAKI,
    EQUIPMENT,
    RACIAL
  }

  public enum Target {
    SELF,
    ENEMY,
    ALL_ENEMIES
  }

  public enum EffectType {
    DAMAGE,
    HEAL,
    SHIELD,
    GUARD,
    EVASION,
    ATTACK_UP,
    ATTACK_DOWN,
    DEFENSE_DOWN,
    POISON,
    STUN,
    ENERGY,
    COATING
  }

  public record Effect(EffectType type, double power, int duration) {
    public Effect {
      if (type == null
          || !Double.isFinite(power)
          || power < 0
          || power > 2
          || duration < 0
          || duration > 4) throw new IllegalArgumentException("Invalid effect");
      if (Set.of(EffectType.HEAL, EffectType.SHIELD).contains(type) && power > .30)
        throw new IllegalArgumentException("Recovery/shield cap is 30% HP");
      if (type == EffectType.STUN && (duration != 1 || power != 0))
        throw new IllegalArgumentException("Control lasts exactly one action");
      if (Set.of(
                  EffectType.GUARD,
                  EffectType.EVASION,
                  EffectType.ATTACK_UP,
                  EffectType.ATTACK_DOWN,
                  EffectType.DEFENSE_DOWN,
                  EffectType.POISON,
                  EffectType.COATING)
              .contains(type)
          && duration < 1) throw new IllegalArgumentException("Status duration required");
      if (type == EffectType.COATING && power != 0)
        throw new IllegalArgumentException("Coating strength comes from profile mastery");
      if (type == EffectType.ENERGY && power > .15)
        throw new IllegalArgumentException("Energy restoration cap is 15%");
      if (Set.of(
                  EffectType.GUARD,
                  EffectType.EVASION,
                  EffectType.ATTACK_UP,
                  EffectType.ATTACK_DOWN,
                  EffectType.DEFENSE_DOWN,
                  EffectType.POISON)
              .contains(type)
          && power > .40) throw new IllegalArgumentException("Status modifier cap is 40%");
    }
  }

  public record Evidence(String url, String note) {
    public Evidence {
      if (url == null
          || !url.startsWith("https://one-piece.com/")
          || note == null
          || note.isBlank())
        throw new IllegalArgumentException("Official source and evidence note required");
    }
  }

  public record Skill(
      String id,
      int version,
      String name,
      String englishName,
      Origin origin,
      Target target,
      int energy,
      int cooldown,
      int maxUses,
      double healthBelow,
      List<String> requiredFruits,
      List<Effect> effects,
      List<Evidence> evidence,
      List<String> tags) {
    public Skill(
        String id,
        int version,
        String name,
        String englishName,
        Origin origin,
        Target target,
        int energy,
        int cooldown,
        int maxUses,
        double healthBelow,
        List<String> requiredFruits,
        List<Effect> effects,
        List<Evidence> evidence) {
      this(
          id,
          version,
          name,
          englishName,
          origin,
          target,
          energy,
          cooldown,
          maxUses,
          healthBelow,
          requiredFruits,
          effects,
          evidence,
          List.of());
    }

    public Skill {
      if (id == null
          || !id.matches("[a-z][a-z0-9_.-]{1,63}")
          || version < 1
          || name == null
          || name.isBlank()
          || name.length() > 60
          || englishName == null
          || englishName.length() > 80
          || origin == null
          || target == null
          || energy < 2
          || energy > 40
          || cooldown < 0
          || cooldown > 8
          || maxUses < 1
          || maxUses > 12
          || !Double.isFinite(healthBelow)
          || healthBelow <= 0
          || healthBelow > 1) throw new IllegalArgumentException("Invalid skill");
      requiredFruits = List.copyOf(requiredFruits == null ? List.of() : requiredFruits);
      effects = List.copyOf(effects == null ? List.of() : effects);
      evidence = List.copyOf(evidence == null ? List.of() : evidence);
      tags = List.copyOf(tags == null ? List.of() : tags);
      if (!INTERACTION_TAGS.containsAll(tags))
        throw new IllegalArgumentException("Unknown skill tags");
      if (effects.isEmpty() || effects.size() > 4 || evidence.isEmpty())
        throw new IllegalArgumentException("Effects and provenance required");
      if (origin == Origin.FRUIT && requiredFruits.isEmpty())
        throw new IllegalArgumentException("Fruit skill needs an explicit fruit requirement");
      if (effects.stream().anyMatch(e -> e.type() == EffectType.COATING) && origin != Origin.HAKI)
        throw new IllegalArgumentException("Coating requires haki");
      if (target == Target.SELF
          && effects.stream()
              .anyMatch(
                  e ->
                      Set.of(
                              EffectType.DAMAGE,
                              EffectType.STUN,
                              EffectType.POISON,
                              EffectType.ATTACK_DOWN,
                              EffectType.DEFENSE_DOWN)
                          .contains(e.type())))
        throw new IllegalArgumentException("Offensive effects target the opponent");
      if (effects.stream()
              .anyMatch(
                  e ->
                      e.type() == EffectType.HEAL
                          || e.type() == EffectType.SHIELD
                          || e.type() == EffectType.GUARD
                          || e.type() == EffectType.ATTACK_UP
                          || e.type() == EffectType.EVASION
                          || e.type() == EffectType.COATING
                          || e.type() == EffectType.ENERGY)
          && target != Target.SELF)
        throw new IllegalArgumentException("Support effects target self in single battles");
    }

    public String label(String locale) {
      return locale != null && locale.startsWith("en") ? englishName : name;
    }
  }

  public record Profile(
      int version,
      String identity,
      String era,
      int coatingLevel,
      List<Skill> skills,
      List<Evidence> evidence,
      List<String> immunities) {
    public Profile(
        int version,
        String identity,
        String era,
        int coatingLevel,
        List<Skill> skills,
        List<Evidence> evidence) {
      this(version, identity, era, coatingLevel, skills, evidence, List.of());
    }

    public Profile {
      if (version < 1
          || identity == null
          || identity.isBlank()
          || era == null
          || era.isBlank()
          || coatingLevel < 0
          || coatingLevel > 3) throw new IllegalArgumentException("Invalid combat profile");
      skills = List.copyOf(skills == null ? List.of() : skills);
      evidence = List.copyOf(evidence == null ? List.of() : evidence);
      immunities = List.copyOf(immunities == null ? List.of() : immunities);
      if (!INTERACTION_TAGS.containsAll(immunities) || !immunities.isEmpty() && evidence.isEmpty())
        throw new IllegalArgumentException("Invalid immunity or missing provenance");
      if (skills.size() > 8 || skills.stream().map(Skill::id).distinct().count() != skills.size())
        throw new IllegalArgumentException("At most eight unique skills");
      if (coatingLevel > 0 && evidence.isEmpty())
        throw new IllegalArgumentException("Coating evidence required");
    }
  }

  public record CatalogEntry(String code, List<String> names, Profile profile) {
    public CatalogEntry {
      names = List.copyOf(names);
    }
  }

  public record Catalog(int version, List<CatalogEntry> characters) {
    public Catalog {
      characters = List.copyOf(characters);
    }
  }

  public record Tactics(String source, Map<String, List<String>> priorities) {
    public Tactics {
      var safe = new LinkedHashMap<String, List<String>>();
      priorities.forEach((k, v) -> safe.put(k, List.copyOf(v)));
      priorities = Map.copyOf(safe);
    }

    public static Tactics rules() {
      return new Tactics("RULES", Map.of());
    }
  }

  public record Outcome(String type, String target, int amount, int duration, String unit) {
    public Outcome(String type, String target, int amount, int duration) {
      this(type, target, amount, duration, "HP");
    }
  }

  public record Event(
      int round,
      String actor,
      String skillId,
      int skillVersion,
      String label,
      boolean missed,
      int energySpent,
      List<Outcome> effects) {
    public Event {
      effects = List.copyOf(effects);
    }
  }
}
