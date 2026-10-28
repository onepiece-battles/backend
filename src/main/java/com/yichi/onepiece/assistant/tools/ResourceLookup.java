package com.yichi.onepiece.assistant.tools;

import static com.yichi.onepiece.assistant.tools.ToolJson.*;

import com.yichi.onepiece.assistant.agent.ToolContext;
import com.yichi.onepiece.battle.service.BattleService;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Shared ID/name resolution. Ambiguous names are never silently resolved to the first row. */
@Component
public class ResourceLookup {
  final CharacterService characters;
  final FruitService fruits;
  final GroupService groups;
  final BattleService battles;
  final IamService iam;

  public ResourceLookup(
      CharacterService characters,
      FruitService fruits,
      GroupService groups,
      BattleService battles,
      IamService iam) {
    this.characters = characters;
    this.fruits = fruits;
    this.groups = groups;
    this.battles = battles;
    this.iam = iam;
  }

  void check(ResourceType type, Permission... permissions) {
    iam.checkPermissionOfCurrentAccount(type, Set.of(permissions));
  }

  static UUID uuid(String value) {
    try {
      UUID id = UUID.fromString(value);
      return id.toString().equalsIgnoreCase(value) ? id : null;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  UUID resolve(String type, String reference, ToolContext context) {
    if (reference == null || reference.isBlank())
      throw problem("invalid_arguments", "A resource ID or name is required");
    UUID id = uuid(reference.trim());
    if (id != null) return id;
    if ("battle".equals(type))
      throw problem("invalid_arguments", "Use a battle ID from search_battles");
    Map<UUID, Object> found = new LinkedHashMap<>();
    for (LanguageCode language :
        new LanguageCode[] {
          context.language().languageCode(),
          context.language().isChinese() ? LanguageCode.EN_US : LanguageCode.ZH_CN
        }) {
      switch (type) {
        case "character" -> {
          check(ResourceType.CHARACTER, Permission.LIST);
          var response =
              characters.listCharacters(
                  new ListCharacterRequest()
                      .pageIndex(1)
                      .pageSize(30)
                      .sortKeys(new ArrayList<>())
                      .characterFilter(
                          new CharacterFilter()
                              .name(reference)
                              .languageCode(language)
                              .userId(context.userId())
                              .systemResource(true)));
          if (response != null)
            for (var item : nonNull(response.getCharacters())) found.put(item.getId(), item);
        }
        case "fruit" -> {
          check(ResourceType.FRUIT, Permission.LIST);
          var response =
              fruits.listFruit(
                  new ListFruitRequest()
                      .pageIndex(1)
                      .pageSize(30)
                      .sortKeys(new ArrayList<>())
                      .fruitFilter(
                          new FruitFilter()
                              .name(reference)
                              .languageCode(language)
                              .userId(context.userId())
                              .systemResource(true)));
          if (response != null)
            for (var item : nonNull(response.getFruits())) found.put(item.getId(), item);
        }
        case "group" -> {
          check(ResourceType.GROUP, Permission.LIST);
          var response =
              groups.list(
                  new ListGroupRequest()
                      .pageIndex(1)
                      .pageSize(30)
                      .sortKeys(new ArrayList<>())
                      .groupFilter(
                          new GroupFilter()
                              .name(reference)
                              .languageCode(language)
                              .userId(context.userId())
                              .systemResource(true)));
          if (response != null)
            for (var item : nonNull(response.getGroups())) found.put(item.getId(), item);
        }
        default -> throw problem("invalid_arguments", "Unknown resource type");
      }
    }
    Optional<UUID> result =
        switch (type) {
          case "character" ->
              singleMatch(
                      found.values().stream().map(CharacterListView.class::cast).toList(),
                      reference,
                      type,
                      CharacterListView::getId,
                      CharacterListView::getName,
                      context.language())
                  .map(CharacterListView::getId);
          case "fruit" ->
              singleMatch(
                      found.values().stream().map(FruitListView.class::cast).toList(),
                      reference,
                      type,
                      FruitListView::getId,
                      FruitListView::getName,
                      context.language())
                  .map(FruitListView::getId);
          case "group" ->
              singleMatch(
                      found.values().stream().map(GroupListView.class::cast).toList(),
                      reference,
                      type,
                      GroupListView::getId,
                      GroupListView::getName,
                      context.language())
                  .map(GroupListView::getId);
          default -> Optional.empty();
        };
    return result.orElseThrow(() -> problem("not_found", "No matching " + type + " was found"));
  }

  CharacterFullView character(String reference, ToolContext context) {
    for (ResourceType type :
        List.of(
            ResourceType.CHARACTER, ResourceType.FRUIT, ResourceType.GROUP, ResourceType.BATTLE)) {
      check(type, Permission.GET);
    }
    return characters.getCharacterFullView(resolve("character", reference, context));
  }

  FruitFullView fruit(String reference, ToolContext context) {
    check(ResourceType.FRUIT, Permission.GET);
    check(ResourceType.CHARACTER, Permission.GET);
    return fruits.getFruit(new GetFruitRequest().fruitId(resolve("fruit", reference, context)));
  }

  GroupFullView group(String reference, ToolContext context) {
    check(ResourceType.GROUP, Permission.GET);
    check(ResourceType.CHARACTER, Permission.GET);
    return groups.getGroupFullView(new GetGroupRequest().id(resolve("group", reference, context)));
  }

  BattleFullView battle(String reference, ToolContext context) {
    check(ResourceType.BATTLE, Permission.GET);
    return battles.getBattleFullView(
        new GetBattleFullViewRequest().battleId(resolve("battle", reference, context)));
  }
}
