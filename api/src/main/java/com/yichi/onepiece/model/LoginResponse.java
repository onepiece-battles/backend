package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.*;
import java.util.Objects;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/** LoginResponse */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-05-03T15:23:47.500171+08:00[Asia/Shanghai]")
public class LoginResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("user_info")
  private UserInfo userInfo;

  @JsonProperty("verification")
  private I18nText verification;

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("refresh_token")
  private String refreshToken;

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

  public LoginResponse verification(I18nText verification) {
    this.verification = verification;
    return this;
  }

  /**
   * Get verification
   *
   * @return verification
   */
  @Valid
  @Schema(name = "verification", required = false)
  public I18nText getVerification() {
    return verification;
  }

  public void setVerification(I18nText verification) {
    this.verification = verification;
  }

  public LoginResponse accessToken(String accessToken) {
    this.accessToken = accessToken;
    return this;
  }

  /**
   * Get accessToken
   *
   * @return accessToken
   */
  @Schema(name = "access_token", required = false)
  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public LoginResponse refreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
    return this;
  }

  /**
   * Get refreshToken
   *
   * @return refreshToken
   */
  @Schema(name = "refresh_token", required = false)
  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
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
        && Objects.equals(this.verification, loginResponse.verification)
        && Objects.equals(this.accessToken, loginResponse.accessToken)
        && Objects.equals(this.refreshToken, loginResponse.refreshToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userInfo, verification, accessToken, refreshToken);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LoginResponse {\n");
    sb.append("    userInfo: ").append(toIndentedString(userInfo)).append("\n");
    sb.append("    verification: ").append(toIndentedString(verification)).append("\n");
    sb.append("    accessToken: ").append(toIndentedString(accessToken)).append("\n");
    sb.append("    refreshToken: ").append(toIndentedString(refreshToken)).append("\n");
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
