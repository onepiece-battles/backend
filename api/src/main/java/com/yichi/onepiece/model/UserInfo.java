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

/** UserInfo */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-18T01:09:52.923697700+08:00[Asia/Shanghai]")
public class UserInfo implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("user_id")
  private UUID userId;

  @JsonProperty("user_name")
  private String userName;

  @JsonProperty("profile_image")
  private org.springframework.core.io.Resource profileImage;

  @JsonProperty("organization")
  private String organization;

  public UserInfo userId(UUID userId) {
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

  public UserInfo userName(String userName) {
    this.userName = userName;
    return this;
  }

  /**
   * Get userName
   *
   * @return userName
   */
  @NotNull
  @Schema(name = "user_name", required = true)
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public UserInfo profileImage(org.springframework.core.io.Resource profileImage) {
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

  public UserInfo organization(String organization) {
    this.organization = organization;
    return this;
  }

  /**
   * Get organization
   *
   * @return organization
   */
  @Schema(name = "organization", required = false)
  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserInfo userInfo = (UserInfo) o;
    return Objects.equals(this.userId, userInfo.userId)
        && Objects.equals(this.userName, userInfo.userName)
        && Objects.equals(this.profileImage, userInfo.profileImage)
        && Objects.equals(this.organization, userInfo.organization);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, userName, profileImage, organization);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserInfo {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    userName: ").append(toIndentedString(userName)).append("\n");
    sb.append("    profileImage: ").append(toIndentedString(profileImage)).append("\n");
    sb.append("    organization: ").append(toIndentedString(organization)).append("\n");
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
