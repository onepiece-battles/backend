package com.yichi.onepiece.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * RegisterRequest
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-04-12T12:49:11.921555+08:00[Asia/Shanghai]")
public class RegisterRequest   {

  @JsonProperty("user_name")
  private String userName;

  @JsonProperty("password")
  private String password;

  @JsonProperty("confirmed_password")
  private String confirmedPassword;

  @JsonProperty("organization")
  private String organization;

  public RegisterRequest userName(String userName) {
    this.userName = userName;
    return this;
  }

  /**
   * Get userName
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

  public RegisterRequest password(String password) {
    this.password = password;
    return this;
  }

  /**
   * Get password
   * @return password
  */
  @NotNull 
  @Schema(name = "password", required = true)
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public RegisterRequest confirmedPassword(String confirmedPassword) {
    this.confirmedPassword = confirmedPassword;
    return this;
  }

  /**
   * Get confirmedPassword
   * @return confirmedPassword
  */
  @NotNull 
  @Schema(name = "confirmed_password", required = true)
  public String getConfirmedPassword() {
    return confirmedPassword;
  }

  public void setConfirmedPassword(String confirmedPassword) {
    this.confirmedPassword = confirmedPassword;
  }

  public RegisterRequest organization(String organization) {
    this.organization = organization;
    return this;
  }

  /**
   * Get organization
   * @return organization
  */
  @NotNull 
  @Schema(name = "organization", required = true)
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
    RegisterRequest registerRequest = (RegisterRequest) o;
    return Objects.equals(this.userName, registerRequest.userName) &&
        Objects.equals(this.password, registerRequest.password) &&
        Objects.equals(this.confirmedPassword, registerRequest.confirmedPassword) &&
        Objects.equals(this.organization, registerRequest.organization);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userName, password, confirmedPassword, organization);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegisterRequest {\n");
    sb.append("    userName: ").append(toIndentedString(userName)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
    sb.append("    confirmedPassword: ").append(toIndentedString(confirmedPassword)).append("\n");
    sb.append("    organization: ").append(toIndentedString(organization)).append("\n");
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

