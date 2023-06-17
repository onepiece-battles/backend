package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.*;
import javax.annotation.Generated;
import javax.validation.constraints.*;

/** types of battles. */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-18T01:09:52.923697700+08:00[Asia/Shanghai]")
public enum BattleType {
  SINGLE("Single"),

  GROUP("Group");

  private String value;

  BattleType(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static BattleType fromValue(String value) {
    for (BattleType b : BattleType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
