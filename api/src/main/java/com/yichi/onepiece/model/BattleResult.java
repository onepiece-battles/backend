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

/** BattleResult */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-19T21:18:43.064586200+08:00[Asia/Shanghai]")
public class BattleResult implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("winner_id")
  private UUID winnerId;

  @JsonProperty("loser_id")
  private UUID loserId;

  @JsonProperty("winner_remain_blood_percentage")
  private Double winnerRemainBloodPercentage;

  public BattleResult winnerId(UUID winnerId) {
    this.winnerId = winnerId;
    return this;
  }

  /**
   * character id of winner.
   *
   * @return winnerId
   */
  @Valid
  @Schema(name = "winner_id", description = "character id of winner.", required = false)
  public UUID getWinnerId() {
    return winnerId;
  }

  public void setWinnerId(UUID winnerId) {
    this.winnerId = winnerId;
  }

  public BattleResult loserId(UUID loserId) {
    this.loserId = loserId;
    return this;
  }

  /**
   * character id of loser.
   *
   * @return loserId
   */
  @Valid
  @Schema(name = "loser_id", description = "character id of loser.", required = false)
  public UUID getLoserId() {
    return loserId;
  }

  public void setLoserId(UUID loserId) {
    this.loserId = loserId;
  }

  public BattleResult winnerRemainBloodPercentage(Double winnerRemainBloodPercentage) {
    this.winnerRemainBloodPercentage = winnerRemainBloodPercentage;
    return this;
  }

  /**
   * remaining blood/ total blood
   *
   * @return winnerRemainBloodPercentage
   */
  @Schema(
      name = "winner_remain_blood_percentage",
      description = "remaining blood/ total blood",
      required = false)
  public Double getWinnerRemainBloodPercentage() {
    return winnerRemainBloodPercentage;
  }

  public void setWinnerRemainBloodPercentage(Double winnerRemainBloodPercentage) {
    this.winnerRemainBloodPercentage = winnerRemainBloodPercentage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BattleResult battleResult = (BattleResult) o;
    return Objects.equals(this.winnerId, battleResult.winnerId)
        && Objects.equals(this.loserId, battleResult.loserId)
        && Objects.equals(
            this.winnerRemainBloodPercentage, battleResult.winnerRemainBloodPercentage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(winnerId, loserId, winnerRemainBloodPercentage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BattleResult {\n");
    sb.append("    winnerId: ").append(toIndentedString(winnerId)).append("\n");
    sb.append("    loserId: ").append(toIndentedString(loserId)).append("\n");
    sb.append("    winnerRemainBloodPercentage: ")
        .append(toIndentedString(winnerRemainBloodPercentage))
        .append("\n");
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
