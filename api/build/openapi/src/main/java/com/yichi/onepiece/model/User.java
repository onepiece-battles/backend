package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.*;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/** User */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2022-04-05T23:39:08.017521+08:00[Asia/Shanghai]")
public class User {

  @JsonProperty("user_id")
  private UUID userId;

  @JsonProperty("user_name")
  private String userName;

  @JsonProperty("organization")
  private String organization;

  public User userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Get userId
   *
   * @return userId
   */
  @Valid
  @Schema(name = "user_id", required = false)
  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public User userName(String userName) {
    this.userName = userName;
    return this;
  }

  /**
   * Get userName
   *
   * @return userName
   */
  @Schema(name = "user_name", required = false)
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public User organization(String organization) {
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
    User user = (User) o;
    return Objects.equals(this.userId, user.userId)
        && Objects.equals(this.userName, user.userName)
        && Objects.equals(this.organization, user.organization);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, userName, organization);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class User {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    userName: ").append(toIndentedString(userName)).append("\n");
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
