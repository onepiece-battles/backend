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

/** StartBattleRequest */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-20T00:07:20.673543200+08:00[Asia/Shanghai]")
public class StartBattleRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("battle_id")
  private UUID battleId;

  public StartBattleRequest battleId(UUID battleId) {
    this.battleId = battleId;
    return this;
  }

  /**
   * Get battleId
   *
   * @return battleId
   */
  @NotNull
  @Valid
  @Schema(name = "battle_id", required = true)
  public UUID getBattleId() {
    return battleId;
  }

  public void setBattleId(UUID battleId) {
    this.battleId = battleId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StartBattleRequest startBattleRequest = (StartBattleRequest) o;
    return Objects.equals(this.battleId, startBattleRequest.battleId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(battleId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StartBattleRequest {\n");
    sb.append("    battleId: ").append(toIndentedString(battleId)).append("\n");
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
