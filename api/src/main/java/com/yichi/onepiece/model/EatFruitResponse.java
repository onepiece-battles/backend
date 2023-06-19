package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.*;
import java.util.Objects;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/** EatFruitResponse */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-19T21:18:43.064586200+08:00[Asia/Shanghai]")
public class EatFruitResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("character")
  private Character character;

  @JsonProperty("fruit_message")
  private String fruitMessage;

  public EatFruitResponse character(Character character) {
    this.character = character;
    return this;
  }

  /**
   * Get character
   *
   * @return character
   */
  @NotNull
  @Valid
  @Schema(name = "character", required = true)
  public Character getCharacter() {
    return character;
  }

  public void setCharacter(Character character) {
    this.character = character;
  }

  public EatFruitResponse fruitMessage(String fruitMessage) {
    this.fruitMessage = fruitMessage;
    return this;
  }

  /**
   * Get fruitMessage
   *
   * @return fruitMessage
   */
  @NotNull
  @Schema(name = "fruit_message", required = true)
  public String getFruitMessage() {
    return fruitMessage;
  }

  public void setFruitMessage(String fruitMessage) {
    this.fruitMessage = fruitMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EatFruitResponse eatFruitResponse = (EatFruitResponse) o;
    return Objects.equals(this.character, eatFruitResponse.character)
        && Objects.equals(this.fruitMessage, eatFruitResponse.fruitMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(character, fruitMessage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EatFruitResponse {\n");
    sb.append("    character: ").append(toIndentedString(character)).append("\n");
    sb.append("    fruitMessage: ").append(toIndentedString(fruitMessage)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}
