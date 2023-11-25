package com.yichi.onepiece.iam;

import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.UserRole;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionDefinition {
  public static final Logger logger = LoggerFactory.getLogger(PermissionDefinition.class);
  private static final String RESOURCE_PERMISSION_SPLITTER = "_";
  private static final String CHARACTER_PREFIX = "CHARACTER";
  private static final String FRUIT_PREFIX = "FRUIT";
  private static final String BATTLE_PREFIX = "BATTLE";
  private static final String CREATE_SUFFIX = "CREATE";
  private static final String GET_SUFFIX = "GET";
  private static final String LIST_SUFFIX = "LIST";
  private static final String UPDATE_SUFFIX = "UPDATE";
  private static final String DELETE_SUFFIX = "DELETE";

  /*
   rules for permission enum:
   [RESOURCE ENUM]_[SPECIFIC_PERMISSION]
   Note: resource name should not contain any '_', because '_' is used to divide resource and permission
  */
  public enum PermissionEnum {
    // user
    USER_ASSIGN_ROLE,
    USER_CREATE,
    USER_LIST,
    USER_GET,
    USER_UPDATE,
    USER_DELETE,
    // role
    ROLE_CREATE,
    ROLE_GET,
    ROLE_LIST,
    ROLE_UPDATE,
    ROLE_DELETE,
    // character
    CHARACTER_CREATE,
    CHARACTER_GET,
    CHARACTER_LIST,
    CHARACTER_UPDATE,
    CHARACTER_DELETE,

    // fruit
    FRUIT_CREATE,
    FRUIT_GET,
    FRUIT_UPDATE,
    FRUIT_DELETE,

    // battle
    BATTLE_CREATE,
    BATTLE_GET,
    BATTLE_LIST,
    BATTLE_UPDATE,
    BATTLE_DELETE,

    // issue
    ISSUE_CREATE,
    ISSUE_GET,
    ISSUE_UPDATE,
    ISSUE_BATCH_UPDATE,
    ISSUE_LIST
  }

  public static final EnumMap<UserRole, Set<PermissionEnum>> ROLE_TO_PERMISSION_MAP =
      new EnumMap<>(
          Map.ofEntries(
              Map.entry(
                  UserRole.VISITOR,
                  Stream.concat(getAllGetPermissions().stream(), getAllListPermissions().stream())
                      .filter(
                          permissionEnum ->
                              !Set.of(
                                      PermissionEnum.USER_GET,
                                      PermissionEnum.ROLE_GET,
                                      PermissionEnum.USER_LIST,
                                      PermissionEnum.ROLE_LIST,
                                      PermissionEnum.ISSUE_LIST,
                                      PermissionEnum.ISSUE_GET)
                                  .contains(permissionEnum))
                      .collect(Collectors.toSet())),
              Map.entry(
                  UserRole.NORMAL,
                  Stream.concat(
                          Stream.concat(
                              Stream.of(PermissionEnum.USER_GET, PermissionEnum.USER_UPDATE),
                              getAllCharacterPermissions().stream()),
                          Stream.concat(
                              getAllBattlePermissions().stream(),
                              getAllFruitPermissions().stream()))
                      .collect(Collectors.toSet())),
              Map.entry(
                  UserRole.NIKA,
                  Arrays.stream(PermissionEnum.values()).collect(Collectors.toSet())),
              Map.entry(
                  UserRole.ADMIN,
                  Stream.concat(
                          Stream.concat(
                              Stream.of(
                                  PermissionEnum.USER_GET,
                                  PermissionEnum.USER_UPDATE,
                                  PermissionEnum.USER_LIST),
                              getAllCharacterPermissions().stream()),
                          Stream.concat(
                              getAllBattlePermissions().stream(),
                              getAllFruitPermissions().stream()))
                      .collect(Collectors.toSet()))));

  public static Map<String, Set<String>> getResourcePermissionMap() {
    return Arrays.stream(ResourceType.values())
        .map(ResourceType::toString)
        .collect(Collectors.toMap(value -> value, PermissionDefinition::getPermissionByResource));
  }

  public static Map<String, Set<String>> getResourcePermissionMapByPermissionSet(
      Set<PermissionEnum> permissions) {
    Map<String, Set<String>> resourcePermissionMap = new HashMap<>();
    for (var permission : permissions) {
      // resource name should not contain any '_'.
      String[] result = permission.toString().split("_", 2);
      var resourceValue = result[0];
      var permissionValue = result[1];
      var currentSet = resourcePermissionMap.getOrDefault(resourceValue, new HashSet<>());
      currentSet.add(permissionValue);
      resourcePermissionMap.put(resourceValue, currentSet);
    }
    return resourcePermissionMap;
  }

  private static Set<String> getPermissionByResource(String resource) {
    return Arrays.stream(PermissionEnum.values())
        .map(PermissionEnum::toString)
        .filter(permission -> permission.startsWith(resource))
        .map(
            selectedPermission -> {
              if (!selectedPermission.contains(RESOURCE_PERMISSION_SPLITTER)) {
                throw ExceptionUtils.unexpectedPermissionEnumFormatException(
                    "Unexpected permission enum: %s", selectedPermission);
              }
              var resultList = selectedPermission.split(RESOURCE_PERMISSION_SPLITTER, 2);
              return resultList[1];
            })
        .collect(Collectors.toSet());
  }

  public static List<String> getAllPermissions() {
    return Arrays.stream(PermissionEnum.values()).map(Enum::toString).collect(Collectors.toList());
  }

  public static Set<PermissionEnum> getAllPermissionEnums() {
    return Arrays.stream(PermissionEnum.values()).collect(Collectors.toSet());
  }

  public static Set<PermissionEnum> getAllCharacterPermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().startsWith(CHARACTER_PREFIX))
        .collect(Collectors.toSet());
  }

  public static Set<PermissionEnum> getAllFruitPermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().startsWith(FRUIT_PREFIX))
        .collect(Collectors.toSet());
  }

  public static Set<PermissionEnum> getAllBattlePermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().startsWith(BATTLE_PREFIX))
        .collect(Collectors.toSet());
  }

  public static Set<PermissionEnum> getAllCreatePermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().endsWith(CREATE_SUFFIX))
        .collect(Collectors.toSet());
  }

  public static Set<PermissionEnum> getAllGetPermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().endsWith(GET_SUFFIX))
        .collect(Collectors.toSet());
  }

  public static Set<PermissionEnum> getAllListPermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().endsWith(LIST_SUFFIX))
        .collect(Collectors.toSet());
  }

  public static Set<PermissionEnum> getAllUpdatePermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().endsWith(UPDATE_SUFFIX))
        .collect(Collectors.toSet());
  }

  public static Set<PermissionEnum> getAllDeletePermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().endsWith(DELETE_SUFFIX))
        .collect(Collectors.toSet());
  }
}
