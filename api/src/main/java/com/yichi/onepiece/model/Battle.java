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

/** Battle */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-18T01:09:52.923697700+08:00[Asia/Shanghai]")
public class Battle implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("action_timestamp")
  private Long actionTimestamp;

  @JsonProperty("battle_id")
  private UUID battleId;

  @JsonProperty("user_id")
  private UUID userId;

  @JsonProperty("initiate_character_id")
  private UUID initiateCharacterId;

  @JsonProperty("receiver_character_id")
  private UUID receiverCharacterId;

  @JsonProperty("type")
  private BattleType type;

  public Battle actionTimestamp(Long actionTimestamp) {
    this.actionTimestamp = actionTimestamp;
    return this;
  }

  /**
   * the timestamp this battle is created
   *
   * @return actionTimestamp
   */
  @NotNull
  @Schema(
      name = "action_timestamp",
      description = "the timestamp this battle is created",
      required = true)
  public Long getActionTimestamp() {
    return actionTimestamp;
  }

  public void setActionTimestamp(Long actionTimestamp) {
    this.actionTimestamp = actionTimestamp;
  }

  public Battle battleId(UUID battleId) {
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

  public Battle userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Get userId
   *
   * @return userId
   */
  @NotNull
  @Valid
  @Schema(name = "user_id", required = true)
  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public Battle initiateCharacterId(UUID initiateCharacterId) {
    this.initiateCharacterId = initiateCharacterId;
    return this;
  }

  /**
   * Get initiateCharacterId
   *
   * @return initiateCharacterId
   */
  @NotNull
  @Valid
  @Schema(name = "initiate_character_id", required = true)
  public UUID getInitiateCharacterId() {
    return initiateCharacterId;
  }

  public void setInitiateCharacterId(UUID initiateCharacterId) {
    this.initiateCharacterId = initiateCharacterId;
  }

  public Battle receiverCharacterId(UUID receiverCharacterId) {
    this.receiverCharacterId = receiverCharacterId;
    return this;
  }

  /**
   * Get receiverCharacterId
   *
   * @return receiverCharacterId
   */
  @NotNull
  @Valid
  @Schema(name = "receiver_character_id", required = true)
  public UUID getReceiverCharacterId() {
    return receiverCharacterId;
  }

  public void setReceiverCharacterId(UUID receiverCharacterId) {
    this.receiverCharacterId = receiverCharacterId;
  }

  public Battle type(BattleType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   *
   * @return type
   */
  @NotNull
  @Valid
  @Schema(name = "type", required = true)
  public BattleType getType() {
    return type;
  }

  public void setType(BattleType type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Battle battle = (Battle) o;
    return Objects.equals(this.actionTimestamp, battle.actionTimestamp)
        && Objects.equals(this.battleId, battle.battleId)
        && Objects.equals(this.userId, battle.userId)
        && Objects.equals(this.initiateCharacterId, battle.initiateCharacterId)
        && Objects.equals(this.receiverCharacterId, battle.receiverCharacterId)
        && Objects.equals(this.type, battle.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        actionTimestamp, battleId, userId, initiateCharacterId, receiverCharacterId, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Battle {\n");
    sb.append("    actionTimestamp: ").append(toIndentedString(actionTimestamp)).append("\n");
    sb.append("    battleId: ").append(toIndentedString(battleId)).append("\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    initiateCharacterId: ")
        .append(toIndentedString(initiateCharacterId))
        .append("\n");
    sb.append("    receiverCharacterId: ")
        .append(toIndentedString(receiverCharacterId))
        .append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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
