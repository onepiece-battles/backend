package com.yichi.onepiece.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.yichi.onepiece.model.FruitType;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * UpdateFruitRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-02T23:12:16.773454300+08:00[Asia/Shanghai]")
public class UpdateFruitRequest  implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("id")
  private UUID id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("owner_id")
  private UUID ownerId;

  @JsonProperty("fruit_type")
  private FruitType fruitType;

  @JsonProperty("group_gain_strengthen")
  private Integer groupGainStrengthen;

  @JsonProperty("magic_consume")
  private Integer magicConsume;

  @JsonProperty("attack_strengthen")
  private Integer attackStrengthen;

  @JsonProperty("defense_strengthen")
  private Integer defenseStrengthen;

  @JsonProperty("intelligence_strengthen")
  private Integer intelligenceStrengthen;

  @JsonProperty("blood_strengthen")
  private Integer bloodStrengthen;

  @JsonProperty("speed_strengthen")
  private Integer speedStrengthen;

  public UpdateFruitRequest id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  */
  @Valid 
  @Schema(name = "id", required = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UpdateFruitRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
  */
  
  @Schema(name = "name", required = false)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UpdateFruitRequest ownerId(UUID ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  /**
   * Get ownerId
   * @return ownerId
  */
  @Valid 
  @Schema(name = "owner_id", required = false)
  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public UpdateFruitRequest fruitType(FruitType fruitType) {
    this.fruitType = fruitType;
    return this;
  }

  /**
   * Get fruitType
   * @return fruitType
  */
  @Valid 
  @Schema(name = "fruit_type", required = false)
  public FruitType getFruitType() {
    return fruitType;
  }

  public void setFruitType(FruitType fruitType) {
    this.fruitType = fruitType;
  }

  public UpdateFruitRequest groupGainStrengthen(Integer groupGainStrengthen) {
    this.groupGainStrengthen = groupGainStrengthen;
    return this;
  }

  /**
   * Get groupGainStrengthen
   * @return groupGainStrengthen
  */
  
  @Schema(name = "group_gain_strengthen", required = false)
  public Integer getGroupGainStrengthen() {
    return groupGainStrengthen;
  }

  public void setGroupGainStrengthen(Integer groupGainStrengthen) {
    this.groupGainStrengthen = groupGainStrengthen;
  }

  public UpdateFruitRequest magicConsume(Integer magicConsume) {
    this.magicConsume = magicConsume;
    return this;
  }

  /**
   * Get magicConsume
   * @return magicConsume
  */
  
  @Schema(name = "magic_consume", required = false)
  public Integer getMagicConsume() {
    return magicConsume;
  }

  public void setMagicConsume(Integer magicConsume) {
    this.magicConsume = magicConsume;
  }

  public UpdateFruitRequest attackStrengthen(Integer attackStrengthen) {
    this.attackStrengthen = attackStrengthen;
    return this;
  }

  /**
   * Get attackStrengthen
   * @return attackStrengthen
  */
  
  @Schema(name = "attack_strengthen", required = false)
  public Integer getAttackStrengthen() {
    return attackStrengthen;
  }

  public void setAttackStrengthen(Integer attackStrengthen) {
    this.attackStrengthen = attackStrengthen;
  }

  public UpdateFruitRequest defenseStrengthen(Integer defenseStrengthen) {
    this.defenseStrengthen = defenseStrengthen;
    return this;
  }

  /**
   * Get defenseStrengthen
   * @return defenseStrengthen
  */
  
  @Schema(name = "defense_strengthen", required = false)
  public Integer getDefenseStrengthen() {
    return defenseStrengthen;
  }

  public void setDefenseStrengthen(Integer defenseStrengthen) {
    this.defenseStrengthen = defenseStrengthen;
  }

  public UpdateFruitRequest intelligenceStrengthen(Integer intelligenceStrengthen) {
    this.intelligenceStrengthen = intelligenceStrengthen;
    return this;
  }

  /**
   * Get intelligenceStrengthen
   * @return intelligenceStrengthen
  */
  
  @Schema(name = "intelligence_strengthen", required = false)
  public Integer getIntelligenceStrengthen() {
    return intelligenceStrengthen;
  }

  public void setIntelligenceStrengthen(Integer intelligenceStrengthen) {
    this.intelligenceStrengthen = intelligenceStrengthen;
  }

  public UpdateFruitRequest bloodStrengthen(Integer bloodStrengthen) {
    this.bloodStrengthen = bloodStrengthen;
    return this;
  }

  /**
   * Get bloodStrengthen
   * @return bloodStrengthen
  */
  
  @Schema(name = "blood_strengthen", required = false)
  public Integer getBloodStrengthen() {
    return bloodStrengthen;
  }

  public void setBloodStrengthen(Integer bloodStrengthen) {
    this.bloodStrengthen = bloodStrengthen;
  }

  public UpdateFruitRequest speedStrengthen(Integer speedStrengthen) {
    this.speedStrengthen = speedStrengthen;
    return this;
  }

  /**
   * Get speedStrengthen
   * @return speedStrengthen
  */
  
  @Schema(name = "speed_strengthen", required = false)
  public Integer getSpeedStrengthen() {
    return speedStrengthen;
  }

  public void setSpeedStrengthen(Integer speedStrengthen) {
    this.speedStrengthen = speedStrengthen;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateFruitRequest updateFruitRequest = (UpdateFruitRequest) o;
    return Objects.equals(this.id, updateFruitRequest.id) &&
        Objects.equals(this.name, updateFruitRequest.name) &&
        Objects.equals(this.ownerId, updateFruitRequest.ownerId) &&
        Objects.equals(this.fruitType, updateFruitRequest.fruitType) &&
        Objects.equals(this.groupGainStrengthen, updateFruitRequest.groupGainStrengthen) &&
        Objects.equals(this.magicConsume, updateFruitRequest.magicConsume) &&
        Objects.equals(this.attackStrengthen, updateFruitRequest.attackStrengthen) &&
        Objects.equals(this.defenseStrengthen, updateFruitRequest.defenseStrengthen) &&
        Objects.equals(this.intelligenceStrengthen, updateFruitRequest.intelligenceStrengthen) &&
        Objects.equals(this.bloodStrengthen, updateFruitRequest.bloodStrengthen) &&
        Objects.equals(this.speedStrengthen, updateFruitRequest.speedStrengthen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, ownerId, fruitType, groupGainStrengthen, magicConsume, attackStrengthen, defenseStrengthen, intelligenceStrengthen, bloodStrengthen, speedStrengthen);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateFruitRequest {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    fruitType: ").append(toIndentedString(fruitType)).append("\n");
    sb.append("    groupGainStrengthen: ").append(toIndentedString(groupGainStrengthen)).append("\n");
    sb.append("    magicConsume: ").append(toIndentedString(magicConsume)).append("\n");
    sb.append("    attackStrengthen: ").append(toIndentedString(attackStrengthen)).append("\n");
    sb.append("    defenseStrengthen: ").append(toIndentedString(defenseStrengthen)).append("\n");
    sb.append("    intelligenceStrengthen: ").append(toIndentedString(intelligenceStrengthen)).append("\n");
    sb.append("    bloodStrengthen: ").append(toIndentedString(bloodStrengthen)).append("\n");
    sb.append("    speedStrengthen: ").append(toIndentedString(speedStrengthen)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

