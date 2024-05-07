package com.yichi.onepiece.iam;

import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.UserRole;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionDefinition {
  public static final Logger logger = LoggerFactory.getLogger(PermissionDefinition.class);
  public static final String RESOURCE_PERMISSION_SPLITTER = "__";

  // resource permission full map
  public static final EnumMap<ResourceType, Set<Permission>> resourcePermissionMap = new EnumMap<>(
          Map.ofEntries(
                  //todo(yichi): login action should be removed out of user resource
                  Map.entry(ResourceType.USER, Arrays.stream(Permission.values()).collect(Collectors.toSet())),
                  Map.entry(ResourceType.CHARACTER, Arrays.stream(Permission.values()).filter(value-> !value.equals(Permission.ASSIGN_ROLE)).collect(Collectors.toSet())),
                  Map.entry(ResourceType.BATTLE, Arrays.stream(Permission.values()).filter(value-> !value.equals(Permission.ASSIGN_ROLE)).collect(Collectors.toSet())),
                  Map.entry(ResourceType.FRUIT, Arrays.stream(Permission.values()).filter(value-> !value.equals(Permission.ASSIGN_ROLE)).collect(Collectors.toSet())),
                  Map.entry(ResourceType.ISSUE, Arrays.stream(Permission.values()).filter(value-> !value.equals(Permission.ASSIGN_ROLE)).collect(Collectors.toSet())),
                  Map.entry(ResourceType.GROUP, Arrays.stream(Permission.values()).filter(value-> !value.equals(Permission.ASSIGN_ROLE)).collect(Collectors.toSet())),
                  Map.entry(ResourceType.ROLE, Arrays.stream(Permission.values()).filter(value-> !value.equals(Permission.ASSIGN_ROLE)).collect(Collectors.toSet()))
          )
  );

  public static final List<Pair<ResourceType, Permission>> resourcePermissionList = resourcePermissionMap.entrySet().stream().map(entry->{
    var values = entry.getValue();
    return values.stream().map(permission -> Pair.of(entry.getKey(), permission)).collect(
        Collectors.toList());
  }).flatMap(Collection::stream).collect(Collectors.toList());

  public static final List<String> resourcePermissionCombinedList = resourcePermissionList.stream().map(rp-> String.format("%s%s%s", rp.getKey(),RESOURCE_PERMISSION_SPLITTER, rp.getValue())).collect(
      Collectors.toList());

  public static List<Pair<ResourceType, Permission>> getPermissionsByRole(UserRole userRole) {
    if (userRole == UserRole.NIKA){
      return getNikaPermissions();
    } else if(userRole == UserRole.ADMIN) {
      return getAdminPermissions();
    } else if(userRole == UserRole.NORMAL) {
      return getNormalUserPermissions();
    } else if(userRole == UserRole.VISITOR) {
      return getVisitorUserPermissions();
    }
    return List.of();
  }

  // role resource permission initial map
  private static List<Pair<ResourceType, Permission>> getNikaPermissions() {
    return resourcePermissionList;
  }

  private static List<Pair<ResourceType, Permission>> getAdminPermissions() {
    return resourcePermissionList.stream().filter(pair ->
      pair.getKey()!=ResourceType.ROLE &&pair.getValue()!=Permission.DELETE &&pair.getValue()!=Permission.ASSIGN_ROLE
    ).toList();
  }

  private static List<Pair<ResourceType, Permission>> getNormalUserPermissions() {
    return getAdminPermissions().stream().filter(rp-> rp.getKey()!=ResourceType.ISSUE).toList();
  }

  private static List<Pair<ResourceType, Permission>> getVisitorUserPermissions() {
    return getNormalUserPermissions().stream().filter(
            rp-> rp.getValue()!=Permission.UPDATE
                    &&rp.getValue()!=Permission.BATCH_UPDATE
            && rp.getValue()!=Permission.CREATE

    ).toList();
  }
}
