package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.*;
import javax.annotation.Generated;
import javax.validation.constraints.*;

/** types that defines how character fight in battles. */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-20T00:07:20.673543200+08:00[Asia/Shanghai]")
public enum CharacterType {
  KATANA("Katana"),

  FIST("Fist"),

  FOOT("Foot"),

  SHOOTER("Shooter"),

  BODY("Body"),

  SCIENCE("Science");

  private String value;

  CharacterType(String value) {
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
  public static CharacterType fromValue(String value) {
    for (CharacterType b : CharacterType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
