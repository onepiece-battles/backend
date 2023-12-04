package com.yichi.onepiece.iam;

import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.model.Permission;
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



  // resource permission full map
  public static final EnumMap<ResourceType, Set<Permission>> resourcePermissionMap = new EnumMap<>(
          Map.ofEntries(
                  Map.entry(ResourceType.USER, Arrays.stream(Permission.values()).collect(Collectors.toSet())),
                  Map.entry(ResourceType.CHARACTER, Arrays.stream(Permission.values()).filter(value-> !value.equals(Permission.ASSIGN_ROLE)).collect(Collectors.toSet())),
                  Map.entry(ResourceType.BATTLE, Arrays.stream(Permission.values()).filter(value-> !value.equals(Permission.ASSIGN_ROLE)).collect(Collectors.toSet())),
                  Map.entry(ResourceType.FRUIT, Arrays.stream(Permission.values()).filter(value-> !value.equals(Permission.ASSIGN_ROLE)).collect(Collectors.toSet())),
                  Map.entry(ResourceType.ISSUE, Arrays.stream(Permission.values()).filter(value-> !value.equals(Permission.ASSIGN_ROLE)).collect(Collectors.toSet())),
                  Map.entry(ResourceType.ROLE, Arrays.stream(Permission.values()).filter(value-> !value.equals(Permission.ASSIGN_ROLE)).collect(Collectors.toSet()))
          )
  );

  // resource permission initial map


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
