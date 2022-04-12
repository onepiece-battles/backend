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
 * RegisterResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-04-12T12:49:11.921555+08:00[Asia/Shanghai]")
public class RegisterResponse   {

  @JsonProperty("regirster_result")
  private Boolean regirsterResult;

  @JsonProperty("verification")
  private String verification;

  public RegisterResponse regirsterResult(Boolean regirsterResult) {
    this.regirsterResult = regirsterResult;
    return this;
  }

  /**
   * Whether user has successfully registered into system.
   * @return regirsterResult
  */
  
  @Schema(name = "regirster_result", description = "Whether user has successfully registered into system.", required = false)
  public Boolean getRegirsterResult() {
    return regirsterResult;
  }

  public void setRegirsterResult(Boolean regirsterResult) {
    this.regirsterResult = regirsterResult;
  }

  public RegisterResponse verification(String verification) {
    this.verification = verification;
    return this;
  }

  /**
   * OK if successfully registered, otherwise explain why register failed.
   * @return verification
  */
  @NotNull 
  @Schema(name = "verification", description = "OK if successfully registered, otherwise explain why register failed.", required = true)
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
    RegisterResponse registerResponse = (RegisterResponse) o;
    return Objects.equals(this.regirsterResult, registerResponse.regirsterResult) &&
        Objects.equals(this.verification, registerResponse.verification);
  }

  @Override
  public int hashCode() {
    return Objects.hash(regirsterResult, verification);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegisterResponse {\n");
    sb.append("    regirsterResult: ").append(toIndentedString(regirsterResult)).append("\n");
    sb.append("    verification: ").append(toIndentedString(verification)).append("\n");
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

