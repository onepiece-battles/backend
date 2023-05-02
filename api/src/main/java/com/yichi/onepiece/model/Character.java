package com.yichi.onepiece.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.yichi.onepiece.model.CharacterType;
import com.yichi.onepiece.model.FruitInCharacter;
import java.util.ArrayList;
import java.util.List;
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
 * Character that contains all necessary information. 
 */

@Schema(name = "Character", description = "Character that contains all necessary information. ")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-02T23:12:16.773454300+08:00[Asia/Shanghai]")
public class Character  implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("id")
  private UUID id;

  @JsonProperty("character_type")
  private CharacterType characterType;

  @JsonProperty("name")
  private String name;

  @JsonProperty("group_oid")
  private String groupOid;

  @JsonProperty("has_fruits")
  private Boolean hasFruits;

  @JsonProperty("fruits")
  @Valid
  private List<FruitInCharacter> fruits = null;

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

  public Character id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
  */
  @NotNull @Valid 
  @Schema(name = "id", required = true)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Character characterType(CharacterType characterType) {
    this.characterType = characterType;
    return this;
  }

  /**
   * Get characterType
   * @return characterType
  */
  @NotNull @Valid 
  @Schema(name = "character_type", required = true)
  public CharacterType getCharacterType() {
    return characterType;
  }

  public void setCharacterType(CharacterType characterType) {
    this.characterType = characterType;
  }

  public Character name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
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

  public Character groupOid(String groupOid) {
    this.groupOid = groupOid;
    return this;
  }

  /**
   * Which group this character is in.
   * @return groupOid
  */
  
  @Schema(name = "group_oid", description = "Which group this character is in.", required = false)
  public String getGroupOid() {
    return groupOid;
  }

  public void setGroupOid(String groupOid) {
    this.groupOid = groupOid;
  }

  public Character hasFruits(Boolean hasFruits) {
    this.hasFruits = hasFruits;
    return this;
  }

  /**
   * Whether this character has eaten fruit
   * @return hasFruits
  */
  @NotNull 
  @Schema(name = "has_fruits", description = "Whether this character has eaten fruit", required = true)
  public Boolean getHasFruits() {
    return hasFruits;
  }

  public void setHasFruits(Boolean hasFruits) {
    this.hasFruits = hasFruits;
  }

  public Character fruits(List<FruitInCharacter> fruits) {
    this.fruits = fruits;
    return this;
  }

  public Character addFruitsItem(FruitInCharacter fruitsItem) {
    if (this.fruits == null) {
      this.fruits = new ArrayList<>();
    }
    this.fruits.add(fruitsItem);
    return this;
  }

  /**
   * Get fruits
   * @return fruits
  */
  @Valid 
  @Schema(name = "fruits", required = false)
  public List<FruitInCharacter> getFruits() {
    return fruits;
  }

  public void setFruits(List<FruitInCharacter> fruits) {
    this.fruits = fruits;
  }

  public Character fruitLimit(Integer fruitLimit) {
    this.fruitLimit = fruitLimit;
    return this;
  }

  /**
   * Maximum number of fruit this character can eat.
   * minimum: 1
   * maximum: 2
   * @return fruitLimit
  */
  @NotNull @Min(1) @Max(2) 
  @Schema(name = "fruit_limit", description = "Maximum number of fruit this character can eat.", required = true)
  public Integer getFruitLimit() {
    return fruitLimit;
  }

  public void setFruitLimit(Integer fruitLimit) {
    this.fruitLimit = fruitLimit;
  }

  public Character groupGain(Integer groupGain) {
    this.groupGain = groupGain;
    return this;
  }

  /**
   * Get groupGain
   * minimum: 1
   * maximum: 5
   * @return groupGain
  */
  @NotNull @Min(1) @Max(5) 
  @Schema(name = "group_gain", required = true)
  public Integer getGroupGain() {
    return groupGain;
  }

  public void setGroupGain(Integer groupGain) {
    this.groupGain = groupGain;
  }

  public Character attack(Integer attack) {
    this.attack = attack;
    return this;
  }

  /**
   * Get attack
   * minimum: 10
   * maximum: 1000
   * @return attack
  */
  @NotNull @Min(10) @Max(1000) 
  @Schema(name = "attack", required = true)
  public Integer getAttack() {
    return attack;
  }

  public void setAttack(Integer attack) {
    this.attack = attack;
  }

  public Character defense(Integer defense) {
    this.defense = defense;
    return this;
  }

  /**
   * Get defense
   * minimum: 10
   * maximum: 1000
   * @return defense
  */
  @NotNull @Min(10) @Max(1000) 
  @Schema(name = "defense", required = true)
  public Integer getDefense() {
    return defense;
  }

  public void setDefense(Integer defense) {
    this.defense = defense;
  }

  public Character intelligence(Integer intelligence) {
    this.intelligence = intelligence;
    return this;
  }

  /**
   * Get intelligence
   * minimum: 70
   * maximum: 260
   * @return intelligence
  */
  @NotNull @Min(70) @Max(260) 
  @Schema(name = "intelligence", required = true)
  public Integer getIntelligence() {
    return intelligence;
  }

  public void setIntelligence(Integer intelligence) {
    this.intelligence = intelligence;
  }

  public Character blood(Integer blood) {
    this.blood = blood;
    return this;
  }

  /**
   * Get blood
   * minimum: 100
   * maximum: 10000
   * @return blood
  */
  @NotNull @Min(100) @Max(10000) 
  @Schema(name = "blood", required = true)
  public Integer getBlood() {
    return blood;
  }

  public void setBlood(Integer blood) {
    this.blood = blood;
  }

  public Character speed(Integer speed) {
    this.speed = speed;
    return this;
  }

  /**
   * Get speed
   * minimum: 10
   * maximum: 200
   * @return speed
  */
  @NotNull @Min(10) @Max(200) 
  @Schema(name = "speed", required = true)
  public Integer getSpeed() {
    return speed;
  }

  public void setSpeed(Integer speed) {
    this.speed = speed;
  }

  public Character magic(Integer magic) {
    this.magic = magic;
    return this;
  }

  /**
   * Get magic
   * minimum: 10
   * maximum: 50
   * @return magic
  */
  @NotNull @Min(10) @Max(50) 
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
    Character character = (Character) o;
    return Objects.equals(this.id, character.id) &&
        Objects.equals(this.characterType, character.characterType) &&
        Objects.equals(this.name, character.name) &&
        Objects.equals(this.groupOid, character.groupOid) &&
        Objects.equals(this.hasFruits, character.hasFruits) &&
        Objects.equals(this.fruits, character.fruits) &&
        Objects.equals(this.fruitLimit, character.fruitLimit) &&
        Objects.equals(this.groupGain, character.groupGain) &&
        Objects.equals(this.attack, character.attack) &&
        Objects.equals(this.defense, character.defense) &&
        Objects.equals(this.intelligence, character.intelligence) &&
        Objects.equals(this.blood, character.blood) &&
        Objects.equals(this.speed, character.speed) &&
        Objects.equals(this.magic, character.magic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, characterType, name, groupOid, hasFruits, fruits, fruitLimit, groupGain, attack, defense, intelligence, blood, speed, magic);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Character {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    characterType: ").append(toIndentedString(characterType)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    groupOid: ").append(toIndentedString(groupOid)).append("\n");
    sb.append("    hasFruits: ").append(toIndentedString(hasFruits)).append("\n");
    sb.append("    fruits: ").append(toIndentedString(fruits)).append("\n");
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

