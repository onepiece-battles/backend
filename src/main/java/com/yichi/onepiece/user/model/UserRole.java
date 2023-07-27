package com.yichi.onepiece.user.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum UserRole {
  ADMIN,
  USER,
  VISITOR,
  NIKA;

  public static Set<String> getRoleNames() {
    return Arrays.stream(UserRole.values()).map(Enum::name).collect(Collectors.toSet());
  }
}
