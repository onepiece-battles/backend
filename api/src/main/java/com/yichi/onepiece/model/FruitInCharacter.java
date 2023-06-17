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

/** describe the fruit condition after it is eaten by character */
@Schema(
    name = "FruitInCharacter",
    description = "describe the fruit condition after it is eaten by character ")
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-18T01:09:52.923697700+08:00[Asia/Shanghai]")
public class FruitInCharacter implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("fruit_id")
  private UUID fruitId;

  @JsonProperty("wake_up")
  private Boolean wakeUp;

  public FruitInCharacter fruitId(UUID fruitId) {
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

  public FruitInCharacter wakeUp(Boolean wakeUp) {
    this.wakeUp = wakeUp;
    return this;
  }

  /**
   * Get wakeUp
   *
   * @return wakeUp
   */
  @NotNull
  @Schema(name = "wake_up", required = true)
  public Boolean getWakeUp() {
    return wakeUp;
  }

  public void setWakeUp(Boolean wakeUp) {
    this.wakeUp = wakeUp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FruitInCharacter fruitInCharacter = (FruitInCharacter) o;
    return Objects.equals(this.fruitId, fruitInCharacter.fruitId)
        && Objects.equals(this.wakeUp, fruitInCharacter.wakeUp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fruitId, wakeUp);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FruitInCharacter {\n");
    sb.append("    fruitId: ").append(toIndentedString(fruitId)).append("\n");
    sb.append("    wakeUp: ").append(toIndentedString(wakeUp)).append("\n");
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
