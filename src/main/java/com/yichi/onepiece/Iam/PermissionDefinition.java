package com.yichi.onepiece.Iam;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionDefinition {
  public static final Logger logger = LoggerFactory.getLogger(PermissionDefinition.class);
  private static final String CHARACTER_PREFIX = "CHARACTER";
  private static final String FRUIT_PREFIX = "FRUIT";
  private static final String BATTLE_PREFIX = "BATTLE";
  private static final String CREATE_SUFFIX = "CREATE";
  private static final String READ_SUFFIX = "READ";
  private static final String UPDATE_SUFFIX = "UPDATE";
  private static final String DELETE_SUFFIX = "DELETE";

  public enum PermissionEnum {
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

  public static List<PermissionEnum> getAllReadPermissions() {
    return Arrays.stream(PermissionEnum.values())
        .filter(value -> value.toString().endsWith(READ_SUFFIX))
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
