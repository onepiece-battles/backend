package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.*;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/** EatFruitRequest */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-05-03T15:23:47.500171+08:00[Asia/Shanghai]")
public class EatFruitRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("character_id")
  private UUID characterId;

  @JsonProperty("fruit_id")
  private UUID fruitId;

  public EatFruitRequest characterId(UUID characterId) {
    this.characterId = characterId;
    return this;
  }

  /**
   * Get characterId
   *
   * @return characterId
   */
  @NotNull
  @Valid
  @Schema(name = "character_id", required = true)
  public UUID getCharacterId() {
    return characterId;
  }

  public void setCharacterId(UUID characterId) {
    this.characterId = characterId;
  }

  public EatFruitRequest fruitId(UUID fruitId) {
    this.fruitId = fruitId;
    return this;
  }

  /**
   * Get fruitId
   *
   * @return fruitId
   */
  @NotNull
  @Valid
  @Schema(name = "fruit_id", required = true)
  public UUID getFruitId() {
    return fruitId;
  }

  public void setFruitId(UUID fruitId) {
    this.fruitId = fruitId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EatFruitRequest eatFruitRequest = (EatFruitRequest) o;
    return Objects.equals(this.characterId, eatFruitRequest.characterId)
        && Objects.equals(this.fruitId, eatFruitRequest.fruitId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(characterId, fruitId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EatFruitRequest {\n");
    sb.append("    characterId: ").append(toIndentedString(characterId)).append("\n");
    sb.append("    fruitId: ").append(toIndentedString(fruitId)).append("\n");
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
