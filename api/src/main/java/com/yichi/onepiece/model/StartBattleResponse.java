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

/** StartBattleResponse */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-19T21:18:43.064586200+08:00[Asia/Shanghai]")
public class StartBattleResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("battle_id")
  private UUID battleId;

  @JsonProperty("result")
  private BattleResult result;

  public StartBattleResponse battleId(UUID battleId) {
    this.battleId = battleId;
    return this;
  }

  /**
   * Get battleId
   *
   * @return battleId
   */
  @Valid
  @Schema(name = "battle_id", required = false)
  public UUID getBattleId() {
    return battleId;
  }

  public void setBattleId(UUID battleId) {
    this.battleId = battleId;
  }

  public StartBattleResponse result(BattleResult result) {
    this.result = result;
    return this;
  }

  /**
   * Get result
   *
   * @return result
   */
  @Valid
  @Schema(name = "result", required = false)
  public BattleResult getResult() {
    return result;
  }

  public void setResult(BattleResult result) {
    this.result = result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StartBattleResponse startBattleResponse = (StartBattleResponse) o;
    return Objects.equals(this.battleId, startBattleResponse.battleId)
        && Objects.equals(this.result, startBattleResponse.result);
  }

  @Override
  public int hashCode() {
    return Objects.hash(battleId, result);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StartBattleResponse {\n");
    sb.append("    battleId: ").append(toIndentedString(battleId)).append("\n");
    sb.append("    result: ").append(toIndentedString(result)).append("\n");
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
