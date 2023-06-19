package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.*;
import javax.annotation.Generated;
import javax.validation.constraints.*;

/** different types of fruit */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-19T21:18:43.064586200+08:00[Asia/Shanghai]")
public enum FruitType {
  PARAMECIA("Paramecia"),

  LOGIA("Logia"),

  ZOAN("Zoan"),

  ANCIENT_ZOAN("Ancient_Zoan"),

  MYTHICAL_ZOAN("Mythical_Zoan"),

  ARTIFICIAL_ZOAN("Artificial_Zoan");

  private String value;

  FruitType(String value) {
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
  public static FruitType fromValue(String value) {
    for (FruitType b : FruitType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
