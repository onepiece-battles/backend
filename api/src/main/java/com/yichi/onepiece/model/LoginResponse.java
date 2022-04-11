package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import javax.annotation.Generated;
import javax.validation.Valid;

/** LoginResponse */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2022-04-11T22:52:51.681056+08:00[Asia/Shanghai]")
public class LoginResponse {

  @JsonProperty("user_info")
  private UserInfo userInfo;

  @JsonProperty("verification")
  private String verification;

  public LoginResponse userInfo(UserInfo userInfo) {
    this.userInfo = userInfo;
    return this;
  }

  /**
   * Get userInfo
   *
   * @return userInfo
   */
  @Valid
  @Schema(name = "user_info", required = false)
  public UserInfo getUserInfo() {
    return userInfo;
  }

  public void setUserInfo(UserInfo userInfo) {
    this.userInfo = userInfo;
  }

  public LoginResponse verification(String verification) {
    this.verification = verification;
    return this;
  }

  /**
   * Get verification
   *
   * @return verification
   */
  @Schema(name = "verification", required = false)
  public String getVerification() {
    return verification;
  }

  public void setVerification(String verification) {
    this.verification = verification;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LoginResponse loginResponse = (LoginResponse) o;
    return Objects.equals(this.userInfo, loginResponse.userInfo)
        && Objects.equals(this.verification, loginResponse.verification);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userInfo, verification);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LoginResponse {\n");
    sb.append("    userInfo: ").append(toIndentedString(userInfo)).append("\n");
    sb.append("    verification: ").append(toIndentedString(verification)).append("\n");
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
