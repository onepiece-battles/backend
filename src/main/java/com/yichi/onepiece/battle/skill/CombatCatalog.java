package com.yichi.onepiece.battle.skill;

import static com.yichi.onepiece.battle.skill.CombatModels.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.model.I18nText;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class CombatCatalog {
  private final Catalog catalog;
  private final Map<String, CatalogEntry> names = new HashMap<>();
  private final Map<String, Profile> previousPresets = new HashMap<>();
  private final Map<String, Profile> currentPresets = new HashMap<>();

  public CombatCatalog() {
    try (var stream = CombatCatalog.class.getResourceAsStream("/battle/catalog-v2.json");
        var legacyStream = CombatCatalog.class.getResourceAsStream("/battle/catalog-v1.json")) {
      if (stream == null) throw new IllegalStateException("Missing skill catalogue");
      catalog = new ObjectMapper().readValue(stream, Catalog.class);
      if (legacyStream == null) throw new IllegalStateException("Missing previous skill catalogue");
      for (var entry : new ObjectMapper().readValue(legacyStream, Catalog.class).characters())
        previousPresets.put(entry.code(), entry.profile());
      var codes = new HashSet<String>();
      for (var entry : catalog.characters()) {
        if (!codes.add(entry.code())) throw new IllegalStateException("Duplicate character code");
        currentPresets.put(entry.code(), entry.profile());
        for (var name : entry.names()) {
          var previous = names.putIfAbsent(normalize(name), entry);
          if (previous != null && !previous.code().equals(entry.code()))
            throw new IllegalStateException("Ambiguous canonical alias: " + name);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Invalid skill catalogue", e);
    }
  }

  public Catalog catalog() {
    return catalog;
  }

  /** Only an untouched factory preset may advance; explicit publications are left alone. */
  public Optional<Profile> replacement(Profile stored) {
    var previous = previousPresets.get(stored.identity());
    var next = currentPresets.get(stored.identity());
    return previous != null
            && previous.equals(stored)
            && next != null
            && next.version() == stored.version() + 1
        ? Optional.of(next)
        : Optional.empty();
  }

  public Optional<CatalogEntry> find(I18nText name) {
    if (name == null) return Optional.empty();
    var zh = names.get(normalize(name.getZhCN()));
    var en = names.get(normalize(name.getEnUS()));
    if (zh != null && en != null && !zh.code().equals(en.code())) return Optional.empty();
    return Optional.ofNullable(zh == null ? en : zh);
  }

  public static String normalize(String name) {
    return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[\\s·・.\\-_]", "");
  }

  public static Profile basic() {
    return new Profile(1, "generic", "unspecified", 0, List.of(), List.of());
  }
}
