package com.yichi.onepiece.iam;

import com.yichi.onepiece.exception.ExceptionUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

  public enum ResourceEnum {
    USER,
    ROLE,
    CHARACTER,
    FRUIT,
    BATTLE
  }

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
    FRUIT_READ,
    FRUIT_UPDATE,
    FRUIT_DELETE,

    // battle
    BATTLE_CREATE,
    BATTLE_GET,
    BATTLE_LIST,
    BATTLE_UPDATE,
    BATTLE_DELETE,
  }

  public static Map<String, Set<String>> getResourcePermissionMap() {
    return Arrays.stream(ResourceEnum.values())
        .map(ResourceEnum::toString)
        .collect(Collectors.toMap(value -> value, PermissionDefinition::getPermissionByResource));
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

  public static List<PermissionDefinition.PermissionEnum> getAllPermissionEnums() {
    return Arrays.stream(PermissionEnum.values()).collect(Collectors.toList());
  }

  public static List<PermissionEnum> getAllCharacterPermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().startsWith(CHARACTER_PREFIX))
        .collect(Collectors.toList());
  }

  public static List<PermissionEnum> getAllFruitPermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().startsWith(FRUIT_PREFIX))
        .collect(Collectors.toList());
  }

  public static List<PermissionEnum> getAllBattlePermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().startsWith(BATTLE_PREFIX))
        .collect(Collectors.toList());
  }

  public static List<PermissionEnum> getAllCreatePermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().endsWith(CREATE_SUFFIX))
        .collect(Collectors.toList());
  }

  public static List<PermissionEnum> getAllGetPermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().endsWith(GET_SUFFIX))
        .collect(Collectors.toList());
  }

  public static List<PermissionEnum> getAllListPermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().endsWith(LIST_SUFFIX))
        .collect(Collectors.toList());
  }

  public static List<PermissionEnum> getAllUpdatePermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().endsWith(UPDATE_SUFFIX))
        .collect(Collectors.toList());
  }

  public static List<PermissionEnum> getAllDeletePermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().endsWith(DELETE_SUFFIX))
        .collect(Collectors.toList());
  }
}
