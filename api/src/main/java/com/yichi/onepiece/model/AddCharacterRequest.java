package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.*;
import java.util.Objects;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/** AddCharacterRequest */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-19T21:18:43.064586200+08:00[Asia/Shanghai]")
public class AddCharacterRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("character_type")
  private CharacterType characterType;

  @JsonProperty("name")
  private String name;

  @JsonProperty("group_oid")
  private String groupOid;

  @JsonProperty("fruit_limit")
  private Integer fruitLimit;

  @JsonProperty("group_gain")
  private Integer groupGain;

  @JsonProperty("attack")
  private Integer attack;

  @JsonProperty("defense")
  private Integer defense;

  @JsonProperty("intelligence")
  private Integer intelligence;

  @JsonProperty("blood")
  private Integer blood;

  @JsonProperty("speed")
  private Integer speed;

  @JsonProperty("magic")
  private Integer magic;

  public AddCharacterRequest characterType(CharacterType characterType) {
    this.characterType = characterType;
    return this;
  }

  /**
   * Get characterType
   *
   * @return characterType
   */
  @NotNull
  @Valid
  @Schema(name = "character_type", required = true)
  public CharacterType getCharacterType() {
    return characterType;
  }

  public void setCharacterType(CharacterType characterType) {
    this.characterType = characterType;
  }

  public AddCharacterRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   *
   * @return name
   */
  @NotNull
  @Schema(name = "name", required = true)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public AddCharacterRequest groupOid(String groupOid) {
    this.groupOid = groupOid;
    return this;
  }

  /**
   * Which group this character is in.
   *
   * @return groupOid
   */
  @Schema(name = "group_oid", description = "Which group this character is in.", required = false)
  public String getGroupOid() {
    return groupOid;
  }

  public void setGroupOid(String groupOid) {
    this.groupOid = groupOid;
  }

  public AddCharacterRequest fruitLimit(Integer fruitLimit) {
    this.fruitLimit = fruitLimit;
    return this;
  }

  /**
   * Maximum number of fruit this character can eat. minimum: 1 maximum: 2
   *
   * @return fruitLimit
   */
  @NotNull
  @Min(1)
  @Max(2)
  @Schema(
      name = "fruit_limit",
      description = "Maximum number of fruit this character can eat.",
      required = true)
  public Integer getFruitLimit() {
    return fruitLimit;
  }

  public void setFruitLimit(Integer fruitLimit) {
    this.fruitLimit = fruitLimit;
  }

  public AddCharacterRequest groupGain(Integer groupGain) {
    this.groupGain = groupGain;
    return this;
  }

  /**
   * Get groupGain minimum: 1 maximum: 5
   *
   * @return groupGain
   */
  @NotNull
  @Min(1)
  @Max(5)
  @Schema(name = "group_gain", required = true)
  public Integer getGroupGain() {
    return groupGain;
  }

  public void setGroupGain(Integer groupGain) {
    this.groupGain = groupGain;
  }

  public AddCharacterRequest attack(Integer attack) {
    this.attack = attack;
    return this;
  }

  /**
   * Get attack minimum: 10 maximum: 1000
   *
   * @return attack
   */
  @NotNull
  @Min(10)
  @Max(1000)
  @Schema(name = "attack", required = true)
  public Integer getAttack() {
    return attack;
  }

  public void setAttack(Integer attack) {
    this.attack = attack;
  }

  public AddCharacterRequest defense(Integer defense) {
    this.defense = defense;
    return this;
  }

  /**
   * Get defense minimum: 10 maximum: 1000
   *
   * @return defense
   */
  @NotNull
  @Min(10)
  @Max(1000)
  @Schema(name = "defense", required = true)
  public Integer getDefense() {
    return defense;
  }

  public void setDefense(Integer defense) {
    this.defense = defense;
  }

  public AddCharacterRequest intelligence(Integer intelligence) {
    this.intelligence = intelligence;
    return this;
  }

  /**
   * Get intelligence minimum: 70 maximum: 260
   *
   * @return intelligence
   */
  @NotNull
  @Min(70)
  @Max(260)
  @Schema(name = "intelligence", required = true)
  public Integer getIntelligence() {
    return intelligence;
  }

  public void setIntelligence(Integer intelligence) {
    this.intelligence = intelligence;
  }

  public AddCharacterRequest blood(Integer blood) {
    this.blood = blood;
    return this;
  }

  /**
   * Get blood minimum: 100 maximum: 10000
   *
   * @return blood
   */
  @NotNull
  @Min(100)
  @Max(10000)
  @Schema(name = "blood", required = true)
  public Integer getBlood() {
    return blood;
  }

  public void setBlood(Integer blood) {
    this.blood = blood;
  }

  public AddCharacterRequest speed(Integer speed) {
    this.speed = speed;
    return this;
  }

  /**
   * Get speed minimum: 10 maximum: 200
   *
   * @return speed
   */
  @NotNull
  @Min(10)
  @Max(200)
  @Schema(name = "speed", required = true)
  public Integer getSpeed() {
    return speed;
  }

  public void setSpeed(Integer speed) {
    this.speed = speed;
  }

  public AddCharacterRequest magic(Integer magic) {
    this.magic = magic;
    return this;
  }

  /**
   * Get magic minimum: 10 maximum: 50
   *
   * @return magic
   */
  @NotNull
  @Min(10)
  @Max(50)
  @Schema(name = "magic", required = true)
  public Integer getMagic() {
    return magic;
  }

  public void setMagic(Integer magic) {
    this.magic = magic;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AddCharacterRequest addCharacterRequest = (AddCharacterRequest) o;
    return Objects.equals(this.characterType, addCharacterRequest.characterType)
        && Objects.equals(this.name, addCharacterRequest.name)
        && Objects.equals(this.groupOid, addCharacterRequest.groupOid)
        && Objects.equals(this.fruitLimit, addCharacterRequest.fruitLimit)
        && Objects.equals(this.groupGain, addCharacterRequest.groupGain)
        && Objects.equals(this.attack, addCharacterRequest.attack)
        && Objects.equals(this.defense, addCharacterRequest.defense)
        && Objects.equals(this.intelligence, addCharacterRequest.intelligence)
        && Objects.equals(this.blood, addCharacterRequest.blood)
        && Objects.equals(this.speed, addCharacterRequest.speed)
        && Objects.equals(this.magic, addCharacterRequest.magic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        characterType,
        name,
        groupOid,
        fruitLimit,
        groupGain,
        attack,
        defense,
        intelligence,
        blood,
        speed,
        magic);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AddCharacterRequest {\n");
    sb.append("    characterType: ").append(toIndentedString(characterType)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    groupOid: ").append(toIndentedString(groupOid)).append("\n");
    sb.append("    fruitLimit: ").append(toIndentedString(fruitLimit)).append("\n");
    sb.append("    groupGain: ").append(toIndentedString(groupGain)).append("\n");
    sb.append("    attack: ").append(toIndentedString(attack)).append("\n");
    sb.append("    defense: ").append(toIndentedString(defense)).append("\n");
    sb.append("    intelligence: ").append(toIndentedString(intelligence)).append("\n");
    sb.append("    blood: ").append(toIndentedString(blood)).append("\n");
    sb.append("    speed: ").append(toIndentedString(speed)).append("\n");
    sb.append("    magic: ").append(toIndentedString(magic)).append("\n");
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
