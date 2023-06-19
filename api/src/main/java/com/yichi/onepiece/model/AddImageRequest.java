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

/** AddImageRequest */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-19T21:18:43.064586200+08:00[Asia/Shanghai]")
public class AddImageRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("id")
  private UUID id;

  @JsonProperty("type")
  private ImageType type;

  @JsonProperty("profile_image")
  private org.springframework.core.io.Resource profileImage;

  public AddImageRequest id(UUID id) {
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

  public AddImageRequest type(ImageType type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   *
   * @return type
   */
  @Valid
  @Schema(name = "type", required = false)
  public ImageType getType() {
    return type;
  }

  public void setType(ImageType type) {
    this.type = type;
  }

  public AddImageRequest profileImage(org.springframework.core.io.Resource profileImage) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AddImageRequest addImageRequest = (AddImageRequest) o;
    return Objects.equals(this.id, addImageRequest.id)
        && Objects.equals(this.type, addImageRequest.type)
        && Objects.equals(this.profileImage, addImageRequest.profileImage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, type, profileImage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AddImageRequest {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    profileImage: ").append(toIndentedString(profileImage)).append("\n");
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
