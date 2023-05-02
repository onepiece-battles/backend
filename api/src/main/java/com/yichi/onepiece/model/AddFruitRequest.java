package com.yichi.onepiece.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.yichi.onepiece.model.FruitType;
import org.openapitools.jackson.nullable.JsonNullable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * AddFruitRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-02T23:12:16.773454300+08:00[Asia/Shanghai]")
public class AddFruitRequest  implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("name")
  private String name;

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

  public AddFruitRequest name(String name) {
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

  public AddFruitRequest fruitType(FruitType fruitType) {
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

  public AddFruitRequest groupGainStrengthen(Integer groupGainStrengthen) {
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

  public AddFruitRequest magicConsume(Integer magicConsume) {
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

  public AddFruitRequest attackStrengthen(Integer attackStrengthen) {
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

  public AddFruitRequest defenseStrengthen(Integer defenseStrengthen) {
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

  public AddFruitRequest intelligenceStrengthen(Integer intelligenceStrengthen) {
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

  public AddFruitRequest bloodStrengthen(Integer bloodStrengthen) {
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

  public AddFruitRequest speedStrengthen(Integer speedStrengthen) {
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
    AddFruitRequest addFruitRequest = (AddFruitRequest) o;
    return Objects.equals(this.name, addFruitRequest.name) &&
        Objects.equals(this.fruitType, addFruitRequest.fruitType) &&
        Objects.equals(this.groupGainStrengthen, addFruitRequest.groupGainStrengthen) &&
        Objects.equals(this.magicConsume, addFruitRequest.magicConsume) &&
        Objects.equals(this.attackStrengthen, addFruitRequest.attackStrengthen) &&
        Objects.equals(this.defenseStrengthen, addFruitRequest.defenseStrengthen) &&
        Objects.equals(this.intelligenceStrengthen, addFruitRequest.intelligenceStrengthen) &&
        Objects.equals(this.bloodStrengthen, addFruitRequest.bloodStrengthen) &&
        Objects.equals(this.speedStrengthen, addFruitRequest.speedStrengthen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, fruitType, groupGainStrengthen, magicConsume, attackStrengthen, defenseStrengthen, intelligenceStrengthen, bloodStrengthen, speedStrengthen);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AddFruitRequest {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

