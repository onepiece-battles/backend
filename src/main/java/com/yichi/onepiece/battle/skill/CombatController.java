package com.yichi.onepiece.battle.skill;

import static com.yichi.onepiece.battle.skill.CombatModels.*;

import com.yichi.onepiece.battle.model.BattleDao;
import com.yichi.onepiece.character.model.dao.CharacterDao;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

/** Additive API; publishing is an explicit NIKA action, never an AI tool side effect. */
@RestController
@RequestMapping("/api/onepiece/combat")
public class CombatController {
  private final CombatCatalog catalog;
  private final CombatProfiles profiles;
  private final CombatSession sessions;
  private final CombatProfileDao profileDao;
  private final CharacterDao characters;
  private final BattleDao battles;
  private final UserService users;
  private final IamService iam;
  private final com.yichi.onepiece.lineage.model.ActionRecordDao lineage;

  public CombatController(
      CombatCatalog catalog,
      CombatProfiles profiles,
      CombatSession sessions,
      CombatProfileDao profileDao,
      CharacterDao characters,
      BattleDao battles,
      UserService users,
      IamService iam,
      com.yichi.onepiece.lineage.model.ActionRecordDao lineage) {
    this.catalog = catalog;
    this.profiles = profiles;
    this.sessions = sessions;
    this.profileDao = profileDao;
    this.characters = characters;
    this.battles = battles;
    this.users = users;
    this.iam = iam;
    this.lineage = lineage;
  }

  @GetMapping("/catalog")
  public Catalog catalog() {
    iam.checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of(Permission.LIST));
    return catalog.catalog();
  }

  @GetMapping("/characters/{id}")
  public Profile character(@PathVariable UUID id) {
    iam.checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of(Permission.GET));
    var character =
        characters
            .findById(id)
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
    readable(
        ResourceType.CHARACTER,
        id,
        Boolean.TRUE.equals(character.getSystemResource()),
        character.getCreationRecordId());
    return profiles.preview(character);
  }

  public record Publish(int expectedVersion, boolean confirmed, Profile profile) {}

  @org.springframework.transaction.annotation.Transactional
  @PostMapping("/characters/{id}/publish")
  public Profile publish(@PathVariable UUID id, @RequestBody Publish request) {
    nika();
    iam.checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of(Permission.UPDATE));
    if (!request.confirmed() || request.profile() == null)
      throw new IllegalArgumentException(
          "Review and explicitly confirm the profile before publishing");
    if (request.expectedVersion() == 1)
      profiles.freeze(
          characters
              .findById(id)
              .orElseThrow(
                  () ->
                      new org.springframework.web.server.ResponseStatusException(
                          org.springframework.http.HttpStatus.NOT_FOUND)));
    return profiles.publish(id, request.expectedVersion(), request.profile());
  }

  @GetMapping("/catalog/matches")
  public List<Map<String, Object>> matches() {
    nika();
    iam.checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of(Permission.LIST));
    var result = new ArrayList<Map<String, Object>>();
    for (UUID id : profileDao.systemCharacters()) {
      var character =
          characters
              .findById(id)
              .orElseThrow(
                  () ->
                      new org.springframework.web.server.ResponseStatusException(
                          org.springframework.http.HttpStatus.NOT_FOUND));
      var match = catalog.find(character.getName());
      result.add(
          Map.of(
              "characterId",
              id,
              "name",
              character.getName(),
              "matched",
              match.isPresent(),
              "identity",
              match.map(CatalogEntry::code).orElse("generic"),
              "profileVersion",
              profiles.preview(character).version()));
    }
    return result;
  }

  @GetMapping("/battles/{id}")
  public Map<String, com.fasterxml.jackson.databind.JsonNode> battle(@PathVariable UUID id) {
    iam.checkPermissionOfCurrentAccount(ResourceType.BATTLE, Set.of(Permission.GET));
    var battle =
        battles
            .findById(id)
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
    readable(
        ResourceType.BATTLE,
        id,
        Boolean.TRUE.equals(battle.getSystemResource()),
        battle.getCreationRecordId());
    return sessions.read(id);
  }

  private static void nika() {
    if (!UserHelper.hasNikaPermission())
      throw new PermissionDeniedException("Only NIKA can publish combat profiles");
  }

  private void readable(ResourceType type, UUID id, boolean system, Long record) {
    if (UserHelper.hasNikaPermission() || system) return;
    // Shared access uses the same explicit resource grants as the resource list.
    var allowed = users.getResourceIdsByUserIdResourceType(UserHelper.getCurrentUserId(), type);
    if (allowed != null && allowed.contains(id)) return;
    // Ownership is verified from lineage, not supplied by a caller.
    if (ownership(record)) return;
    throw new PermissionDeniedException("Resource is not accessible");
  }

  private boolean ownership(Long record) {
    return record != null
        && lineage
            .findById(record)
            .map(r -> UserHelper.getCurrentUserId().equals(r.getUserId()))
            .orElse(false);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public org.springframework.http.ResponseEntity<Map<String, String>> invalid(
      IllegalArgumentException e) {
    return org.springframework.http.ResponseEntity.status(
            e.getMessage() != null && e.getMessage().startsWith("Profile changed") ? 409 : 400)
        .body(
            Map.of("message", e.getMessage() == null ? "Invalid combat profile" : e.getMessage()));
  }
}
