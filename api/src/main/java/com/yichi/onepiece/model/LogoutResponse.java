package com.yichi.onepiece.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openapitools.jackson.nullable.JsonNullable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * LogoutResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-02T23:12:16.773454300+08:00[Asia/Shanghai]")
public class LogoutResponse  implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("user_name")
  private String userName;

  @JsonProperty("logout_result")
  private String logoutResult;

  public LogoutResponse userName(String userName) {
    this.userName = userName;
    return this;
  }

  /**
   * Get userName
   * @return userName
  */
  
  @Schema(name = "user_name", required = false)
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public LogoutResponse logoutResult(String logoutResult) {
    this.logoutResult = logoutResult;
    return this;
  }

  /**
   * whether user has successfully logout.
   * @return logoutResult
  */
  
  @Schema(name = "logout_result", description = "whether user has successfully logout.", required = false)
  public String getLogoutResult() {
    return logoutResult;
  }

  public void setLogoutResult(String logoutResult) {
    this.logoutResult = logoutResult;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LogoutResponse logoutResponse = (LogoutResponse) o;
    return Objects.equals(this.userName, logoutResponse.userName) &&
        Objects.equals(this.logoutResult, logoutResponse.logoutResult);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userName, logoutResult);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LogoutResponse {\n");
    sb.append("    userName: ").append(toIndentedString(userName)).append("\n");
    sb.append("    logoutResult: ").append(toIndentedString(logoutResult)).append("\n");
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

