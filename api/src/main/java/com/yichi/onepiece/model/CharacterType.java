package com.yichi.onepiece.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * types that defines how character fight in battles.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-09T16:50:20.075306800+08:00[Asia/Shanghai]")
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

