package com.yichi.onepiece.battle.skill;

import static com.yichi.onepiece.battle.skill.CombatModels.*;

import com.yichi.onepiece.character.model.dao.HakiDao;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.model.FruitEntity;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.model.Haki;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Collect a consistent database view before any external AI request or simulation. */
@Service
public class CombatInputs {
  public record Frozen(
      CharacterEntity first,
      CharacterEntity second,
      List<FruitEntity> firstFruits,
      List<FruitEntity> secondFruits,
      List<Haki> firstHakis,
      List<Haki> secondHakis,
      Map<UUID, Profile> profiles) {
    public Frozen {
      firstFruits = List.copyOf(firstFruits);
      secondFruits = List.copyOf(secondFruits);
      firstHakis = List.copyOf(firstHakis);
      secondHakis = List.copyOf(secondHakis);
      profiles = Map.copyOf(profiles);
    }
  }

  private final CombatProfileDao locks;
  private final CombatProfiles profiles;
  private final CharacterService characters;
  private final FruitService fruits;
  private final HakiDao hakis;

  public CombatInputs(
      CombatProfileDao locks,
      CombatProfiles profiles,
      CharacterService characters,
      FruitService fruits,
      HakiDao hakis) {
    this.locks = locks;
    this.profiles = profiles;
    this.characters = characters;
    this.fruits = fruits;
    this.hakis = hakis;
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
  public void lockParticipants(UUID first, UUID second) {
    var ordered = new ArrayList<>(List.of(first, second));
    ordered.sort(Comparator.comparing(UUID::toString));
    for (UUID id : ordered)
      if (locks.lockCharacter(id) == null)
        throw new IllegalArgumentException("Missing participant");
  }

  @Transactional
  public Frozen freeze(UUID first, UUID second) {
    if (first.equals(second))
      throw new IllegalArgumentException("Battle needs two distinct characters");
    var ordered = new ArrayList<>(List.of(first, second));
    ordered.sort(Comparator.comparing(UUID::toString));
    for (UUID id : ordered)
      if (locks.lockCharacter(id) == null)
        throw new IllegalArgumentException("Missing participant");
    var a = characters.getCharacterEntityById(first);
    var b = characters.getCharacterEntityById(second);
    var definitions = new LinkedHashMap<UUID, Profile>();
    definitions.put(first, profiles.freeze(a));
    definitions.put(second, profiles.freeze(b));
    return new Frozen(
        a,
        b,
        fruits.listFruitEntityByCharacterId(first),
        fruits.listFruitEntityByCharacterId(second),
        hakis.listByCharacterId(first),
        hakis.listByCharacterId(second),
        definitions);
  }
}
