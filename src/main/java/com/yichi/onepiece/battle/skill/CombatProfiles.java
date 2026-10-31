package com.yichi.onepiece.battle.skill;

import static com.yichi.onepiece.battle.skill.CombatModels.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CombatProfiles {
  private final CombatProfileDao dao;
  private final CombatCatalog catalog;
  private final Clock clock;
  private final ObjectMapper json = new ObjectMapper();

  public CombatProfiles(CombatProfileDao dao, CombatCatalog catalog, Clock clock) {
    this.dao = dao;
    this.catalog = catalog;
    this.clock = clock;
  }

  public Profile preview(CharacterEntity character) {
    return dao.latest(character.getId())
        .map(this::decode)
        .map(p -> current(character, p))
        .orElseGet(() -> initial(character));
  }

  private Profile current(CharacterEntity character, Profile stored) {
    return Boolean.TRUE.equals(character.getSystemResource())
        ? catalog.replacement(stored).orElse(stored)
        : stored;
  }

  private Profile initial(CharacterEntity character) {
    return Boolean.TRUE.equals(character.getSystemResource())
        ? catalog
            .find(character.getName())
            .map(CatalogEntry::profile)
            .orElseGet(CombatCatalog::basic)
        : CombatCatalog.basic();
  }

  @Transactional
  public Profile freeze(CharacterEntity character) {
    if (dao.lockCharacter(character.getId()) == null)
      throw new IllegalArgumentException("Character does not exist");
    var stored = dao.latest(character.getId());
    if (stored.isPresent()) {
      var previous = decode(stored.get());
      var next = current(character, previous);
      if (!next.equals(previous))
        dao.insert(character.getId(), next.version(), encode(next), clock.millis());
      return next;
    }
    var profile = initial(character);
    dao.insert(character.getId(), profile.version(), encode(profile), clock.millis());
    return profile;
  }

  @Transactional
  public Profile publish(UUID id, int expectedVersion, Profile next) {
    if (dao.lockCharacter(id) == null)
      throw new IllegalArgumentException("Character does not exist");
    int current = dao.latest(id).map(this::decode).map(Profile::version).orElse(0);
    if (current != expectedVersion || next.version() != current + 1)
      throw new IllegalArgumentException("Profile changed; reload before publishing");
    var lastSkills = new HashMap<String, Skill>();
    for (String version : dao.history(id))
      for (var skill : decode(version).skills()) lastSkills.put(skill.id(), skill);
    for (var skill : next.skills()) {
      var previous = lastSkills.get(skill.id());
      if (previous == null && skill.version() != 1)
        throw new IllegalArgumentException("New skills start at version 1");
      if (previous != null && !previous.equals(skill) && skill.version() != previous.version() + 1)
        throw new IllegalArgumentException("Changed skills require the next skill version");
    }
    dao.insert(id, next.version(), encode(next), clock.millis());
    return next;
  }

  private Profile decode(String value) {
    try {
      return json.readValue(value, Profile.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Invalid stored profile", e);
    }
  }

  private String encode(Profile value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid profile", e);
    }
  }
}
