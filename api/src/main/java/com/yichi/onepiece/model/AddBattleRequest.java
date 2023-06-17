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

/** AddBattleRequest */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-18T01:09:52.923697700+08:00[Asia/Shanghai]")
public class AddBattleRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("user_id")
  private UUID userId;

  @JsonProperty("initiate_character_id")
  private UUID initiateCharacterId;

  @JsonProperty("receiver_character_id")
  private UUID receiverCharacterId;

  public AddBattleRequest userId(UUID userId) {
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

  public AddBattleRequest initiateCharacterId(UUID initiateCharacterId) {
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

  public AddBattleRequest receiverCharacterId(UUID receiverCharacterId) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AddBattleRequest addBattleRequest = (AddBattleRequest) o;
    return Objects.equals(this.userId, addBattleRequest.userId)
        && Objects.equals(this.initiateCharacterId, addBattleRequest.initiateCharacterId)
        && Objects.equals(this.receiverCharacterId, addBattleRequest.receiverCharacterId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, initiateCharacterId, receiverCharacterId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AddBattleRequest {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    initiateCharacterId: ")
        .append(toIndentedString(initiateCharacterId))
        .append("\n");
    sb.append("    receiverCharacterId: ")
        .append(toIndentedString(receiverCharacterId))
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
