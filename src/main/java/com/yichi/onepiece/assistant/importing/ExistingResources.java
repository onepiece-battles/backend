package com.yichi.onepiece.assistant.importing;

import static com.yichi.onepiece.assistant.importing.ImportDto.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.group.service.GroupService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import java.util.*;
import org.springframework.stereotype.Component;

/**
 * What the import needs to know about resources already in the app: whether a drafted name exists
 * (so it is reused, not duplicated) and existing characters/fruits to calibrate new stats against.
 */
@Component
public class ExistingResources {
  static final int SEARCH_PAGE = 30;
  private final CharacterService characters;
  private final FruitService fruits;
  private final GroupService groups;
  private final IamService iam;
  private final ObjectMapper json = new ObjectMapper();

  public ExistingResources(
      CharacterService characters, FruitService fruits, GroupService groups, IamService iam) {
    this.characters = characters;
    this.fruits = fruits;
    this.groups = groups;
    this.iam = iam;
  }

  /** Ignores case, spaces and the dot variants Chinese names are written with (·・•.). */
  static String normalize(String name) {
    return name == null ? "" : name.replaceAll("[\\s·・•.]", "").toLowerCase(Locale.ROOT);
  }

  static boolean sameName(I18nText existing, I18nText drafted) {
    if (existing == null || drafted == null) return false;
    for (String a : new String[] {existing.getZhCN(), existing.getEnUS()})
      for (String b : new String[] {drafted.getZhCN(), drafted.getEnUS()})
        if (!normalize(a).isEmpty() && normalize(a).equals(normalize(b))) return true;
    return false;
  }

  /**
   * Search terms for a name: the English name, and the last segment of the Chinese one, so
   * "蒙奇·D·路飞" also finds "蒙奇・D・路飞" (the list filters match substrings).
   */
  private static List<Map.Entry<LanguageCode, String>> terms(I18nText name) {
    var terms = new ArrayList<Map.Entry<LanguageCode, String>>();
    if (name.getZhCN() != null && !name.getZhCN().isBlank()) {
      String[] parts = name.getZhCN().trim().split("[\\s·・•.]+");
      terms.add(Map.entry(LanguageCode.ZH_CN, parts[parts.length - 1]));
    }
    if (name.getEnUS() != null && !name.getEnUS().isBlank())
      terms.add(Map.entry(LanguageCode.EN_US, name.getEnUS().trim()));
    return terms;
  }

  /** The id of the resource of this type whose name equals the drafted one, if any. */
  public Optional<UUID> find(String type, I18nText name, UUID userId) {
    for (var term : terms(name)) {
      Optional<UUID> found =
          switch (type) {
            case CHARACTER -> {
              iam.checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of(Permission.LIST));
              var page =
                  characters.listCharacters(
                      new ListCharacterRequest()
                          .pageIndex(1)
                          .pageSize(SEARCH_PAGE)
                          .sortKeys(new ArrayList<>())
                          .characterFilter(
                              new CharacterFilter()
                                  .name(term.getValue())
                                  .languageCode(term.getKey())
                                  .userId(userId)
                                  .systemResource(true)));
              yield page == null
                  ? Optional.empty()
                  : page.getCharacters().stream()
                      .filter(c -> sameName(c.getName(), name))
                      .map(CharacterListView::getId)
                      .findFirst();
            }
            case FRUIT -> {
              iam.checkPermissionOfCurrentAccount(ResourceType.FRUIT, Set.of(Permission.LIST));
              var page =
                  fruits.listFruit(
                      new ListFruitRequest()
                          .pageIndex(1)
                          .pageSize(SEARCH_PAGE)
                          .sortKeys(new ArrayList<>())
                          .fruitFilter(
                              new FruitFilter()
                                  .name(term.getValue())
                                  .languageCode(term.getKey())
                                  .userId(userId)
                                  .systemResource(true)));
              yield page == null
                  ? Optional.empty()
                  : page.getFruits().stream()
                      .filter(f -> sameName(f.getName(), name))
                      .map(FruitListView::getId)
                      .findFirst();
            }
            case GROUP -> {
              iam.checkPermissionOfCurrentAccount(ResourceType.GROUP, Set.of(Permission.LIST));
              var page =
                  groups.list(
                      new ListGroupRequest()
                          .pageIndex(1)
                          .pageSize(SEARCH_PAGE)
                          .sortKeys(new ArrayList<>())
                          .groupFilter(
                              new GroupFilter()
                                  .name(term.getValue())
                                  .languageCode(term.getKey())
                                  .userId(userId)
                                  .systemResource(true)));
              yield page == null
                  ? Optional.empty()
                  : page.getGroups().stream()
                      .filter(g -> sameName(g.getName(), name))
                      .map(GroupListView::getId)
                      .findFirst();
            }
            default -> throw new IllegalArgumentException("Unknown type " + type);
          };
      if (found.isPresent()) return found;
    }
    return Optional.empty();
  }

  /**
   * Up to {@code limit} existing characters spread across the score range, as their base stats
   * (what a create request sets), so the model can place new characters on the app's own scale.
   */
  public List<ObjectNode> referenceCharacters(UUID userId, int limit) {
    iam.checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of(Permission.LIST));
    var page =
        characters.listCharacters(
            new ListCharacterRequest()
                .pageIndex(1)
                .pageSize(60)
                .sortKeys(
                    new ArrayList<>(
                        List.of(
                            new CharacterSortableField()
                                .sortKey(CharacterSortableFieldsEnum.SCORE)
                                .sortOrder(SortOrder.DESC))))
                .characterFilter(new CharacterFilter().userId(userId).systemResource(true)));
    var all = page == null ? List.<CharacterListView>of() : page.getCharacters();
    var rows = new ArrayList<ObjectNode>();
    for (var c : spread(all, limit)) {
      var row = json.createObjectNode();
      row.put("name", c.getName() == null ? "" : firstNonBlank(c.getName()));
      putIfPresent(row, "bounty", c.getBounty());
      putIfPresent(row, "maximum_fruit_count", c.getMaximumFruitCount());
      putIfPresent(row, "group_gain", c.getBaseGroupGain());
      putIfPresent(row, "attack", c.getBaseAttack());
      putIfPresent(row, "defense", c.getBaseDefense());
      putIfPresent(row, "blood", c.getBaseBlood());
      putIfPresent(row, "speed", c.getBaseSpeed());
      putIfPresent(row, "intelligence", c.getBaseIntelligence());
      putIfPresent(row, "energy", c.getEnergy());
      rows.add(row);
    }
    return rows;
  }

  /** Up to {@code limit} existing fruits with their stat coefficients. */
  public List<ObjectNode> referenceFruits(UUID userId, int limit) {
    iam.checkPermissionOfCurrentAccount(ResourceType.FRUIT, Set.of(Permission.LIST));
    var page =
        fruits.listFruit(
            new ListFruitRequest()
                .pageIndex(1)
                .pageSize(limit)
                .sortKeys(new ArrayList<>())
                .fruitFilter(new FruitFilter().userId(userId).systemResource(true)));
    var rows = new ArrayList<ObjectNode>();
    for (var f : page == null ? List.<FruitListView>of() : page.getFruits()) {
      ObjectNode row = json.valueToTree(f);
      row.retain(
          List.of(
              "fruit_type",
              "energy_consume",
              "attack_slope",
              "attack_intercept",
              "defense_slope",
              "defense_intercept",
              "blood_slope",
              "blood_intercept",
              "group_gain_slope",
              "group_gain_intercept",
              "speed_slope",
              "speed_intercept",
              "intelligence_slope",
              "intelligence_intercept"));
      row.put("name", f.getName() == null ? "" : firstNonBlank(f.getName()));
      rows.add(row);
    }
    return rows;
  }

  /** Every k-th element so the sample covers the whole (score-sorted) range. */
  static <T> List<T> spread(List<T> sorted, int limit) {
    if (sorted.size() <= limit) return sorted;
    var picked = new ArrayList<T>();
    for (int i = 0; i < limit; i++) picked.add(sorted.get(i * (sorted.size() - 1) / (limit - 1)));
    return picked;
  }

  private static String firstNonBlank(I18nText name) {
    return name.getZhCN() != null && !name.getZhCN().isBlank() ? name.getZhCN() : name.getEnUS();
  }

  private static void putIfPresent(ObjectNode row, String key, Number value) {
    if (value != null) row.put(key, value.longValue());
  }
}
