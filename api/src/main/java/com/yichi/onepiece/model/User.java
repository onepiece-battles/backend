package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/** User */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-20T00:07:20.673543200+08:00[Asia/Shanghai]")
public class User implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("id")
  private UUID id;

  @JsonProperty("name")
  private String name;

  @JsonProperty("email")
  private String email;

  @JsonProperty("user_roles")
  @Valid
  private List<String> userRoles = null;

  @JsonProperty("profile_image")
  private org.springframework.core.io.Resource profileImage;

  @JsonProperty("organization")
  private String organization;

  public User id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   *
   * @return id
   */
  @NotNull
  @Valid
  @Schema(name = "id", required = true)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public User name(String name) {
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

  public User email(String email) {
    this.email = email;
    return this;
  }

  /**
   * Get email
   *
   * @return email
   */
  @Schema(name = "email", required = false)
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public User userRoles(List<String> userRoles) {
    this.userRoles = userRoles;
    return this;
  }

  public User addUserRolesItem(String userRolesItem) {
    if (this.userRoles == null) {
      this.userRoles = new ArrayList<>();
    }
    this.userRoles.add(userRolesItem);
    return this;
  }

  /**
   * Get userRoles
   *
   * @return userRoles
   */
  @Schema(name = "user_roles", required = false)
  public List<String> getUserRoles() {
    return userRoles;
  }

  public void setUserRoles(List<String> userRoles) {
    this.userRoles = userRoles;
  }

  public User profileImage(org.springframework.core.io.Resource profileImage) {
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
    return Objects.equals(this.id, user.id)
        && Objects.equals(this.name, user.name)
        && Objects.equals(this.email, user.email)
        && Objects.equals(this.userRoles, user.userRoles)
        && Objects.equals(this.profileImage, user.profileImage)
        && Objects.equals(this.organization, user.organization);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, email, userRoles, profileImage, organization);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class User {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    userRoles: ").append(toIndentedString(userRoles)).append("\n");
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
