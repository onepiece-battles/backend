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

/** all information about fruit */
@Schema(name = "Fruit", description = "all information about fruit")
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-20T00:07:20.673543200+08:00[Asia/Shanghai]")
public class Fruit implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("id")
  private UUID id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("profile_image")
  private org.springframework.core.io.Resource profileImage;

  @JsonProperty("owner_id")
  private UUID ownerId;

  @JsonProperty("fruit_type")
  private FruitType fruitType;

  @JsonProperty("magic_consume")
  private Integer magicConsume;

  @JsonProperty("attack_strengthen")
  private Integer attackStrengthen;

  @JsonProperty("intelligence_strengthen")
  private Integer intelligenceStrengthen;

  @JsonProperty("blood_strengthen")
  private Integer bloodStrengthen;

  @JsonProperty("speed_strengthen")
  private Integer speedStrengthen;

  public Fruit id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   *
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

  public Fruit name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   *
   * @return name
   */
  @Schema(name = "name", required = false)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Fruit profileImage(org.springframework.core.io.Resource profileImage) {
    this.profileImage = profileImage;
    return this;
  }

  /**
   * Get profileImage
   *
   * @return profileImage
   */
  @Valid
  @Schema(name = "profile_image", required = false)
  public org.springframework.core.io.Resource getProfileImage() {
    return profileImage;
  }

  public void setProfileImage(org.springframework.core.io.Resource profileImage) {
    this.profileImage = profileImage;
  }

  public Fruit ownerId(UUID ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  /**
   * Get ownerId
   *
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

  public Fruit fruitType(FruitType fruitType) {
    this.fruitType = fruitType;
    return this;
  }

  /**
   * Get fruitType
   *
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

  public Fruit magicConsume(Integer magicConsume) {
    this.magicConsume = magicConsume;
    return this;
  }

  /**
   * Get magicConsume
   *
   * @return magicConsume
   */
  @Schema(name = "magic_consume", required = false)
  public Integer getMagicConsume() {
    return magicConsume;
  }

  public void setMagicConsume(Integer magicConsume) {
    this.magicConsume = magicConsume;
  }

  public Fruit attackStrengthen(Integer attackStrengthen) {
    this.attackStrengthen = attackStrengthen;
    return this;
  }

  /**
   * Get attackStrengthen
   *
   * @return attackStrengthen
   */
  @Schema(name = "attack_strengthen", required = false)
  public Integer getAttackStrengthen() {
    return attackStrengthen;
  }

  public void setAttackStrengthen(Integer attackStrengthen) {
    this.attackStrengthen = attackStrengthen;
  }

  public Fruit intelligenceStrengthen(Integer intelligenceStrengthen) {
    this.intelligenceStrengthen = intelligenceStrengthen;
    return this;
  }

  /**
   * Get intelligenceStrengthen
   *
   * @return intelligenceStrengthen
   */
  @Schema(name = "intelligence_strengthen", required = false)
  public Integer getIntelligenceStrengthen() {
    return intelligenceStrengthen;
  }

  public void setIntelligenceStrengthen(Integer intelligenceStrengthen) {
    this.intelligenceStrengthen = intelligenceStrengthen;
  }

  public Fruit bloodStrengthen(Integer bloodStrengthen) {
    this.bloodStrengthen = bloodStrengthen;
    return this;
  }

  /**
   * Get bloodStrengthen
   *
   * @return bloodStrengthen
   */
  @Schema(name = "blood_strengthen", required = false)
  public Integer getBloodStrengthen() {
    return bloodStrengthen;
  }

  public void setBloodStrengthen(Integer bloodStrengthen) {
    this.bloodStrengthen = bloodStrengthen;
  }

  public Fruit speedStrengthen(Integer speedStrengthen) {
    this.speedStrengthen = speedStrengthen;
    return this;
  }

  /**
   * Get speedStrengthen
   *
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
    Fruit fruit = (Fruit) o;
    return Objects.equals(this.id, fruit.id)
        && Objects.equals(this.name, fruit.name)
        && Objects.equals(this.profileImage, fruit.profileImage)
        && Objects.equals(this.ownerId, fruit.ownerId)
        && Objects.equals(this.fruitType, fruit.fruitType)
        && Objects.equals(this.magicConsume, fruit.magicConsume)
        && Objects.equals(this.attackStrengthen, fruit.attackStrengthen)
        && Objects.equals(this.intelligenceStrengthen, fruit.intelligenceStrengthen)
        && Objects.equals(this.bloodStrengthen, fruit.bloodStrengthen)
        && Objects.equals(this.speedStrengthen, fruit.speedStrengthen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        name,
        profileImage,
        ownerId,
        fruitType,
        magicConsume,
        attackStrengthen,
        intelligenceStrengthen,
        bloodStrengthen,
        speedStrengthen);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Fruit {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    profileImage: ").append(toIndentedString(profileImage)).append("\n");
    sb.append("    ownerId: ").append(toIndentedString(ownerId)).append("\n");
    sb.append("    fruitType: ").append(toIndentedString(fruitType)).append("\n");
    sb.append("    magicConsume: ").append(toIndentedString(magicConsume)).append("\n");
    sb.append("    attackStrengthen: ").append(toIndentedString(attackStrengthen)).append("\n");
    sb.append("    intelligenceStrengthen: ")
        .append(toIndentedString(intelligenceStrengthen))
        .append("\n");
    sb.append("    bloodStrengthen: ").append(toIndentedString(bloodStrengthen)).append("\n");
    sb.append("    speedStrengthen: ").append(toIndentedString(speedStrengthen)).append("\n");
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
