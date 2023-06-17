package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.*;
import java.util.Objects;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/** RegisterResponse */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-18T01:09:52.923697700+08:00[Asia/Shanghai]")
public class RegisterResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("register_result")
  private Boolean registerResult;

  @JsonProperty("verification")
  private I18nText verification;

  public RegisterResponse registerResult(Boolean registerResult) {
    this.registerResult = registerResult;
    return this;
  }

  /**
   * Whether user has successfully registered into system.
   *
   * @return registerResult
   */
  @NotNull
  @Schema(
      name = "register_result",
      description = "Whether user has successfully registered into system.",
      required = true)
  public Boolean getRegisterResult() {
    return registerResult;
  }

  public void setRegisterResult(Boolean registerResult) {
    this.registerResult = registerResult;
  }

  public RegisterResponse verification(I18nText verification) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RegisterResponse registerResponse = (RegisterResponse) o;
    return Objects.equals(this.registerResult, registerResponse.registerResult)
        && Objects.equals(this.verification, registerResponse.verification);
  }

  @Override
  public int hashCode() {
    return Objects.hash(registerResult, verification);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegisterResponse {\n");
    sb.append("    registerResult: ").append(toIndentedString(registerResult)).append("\n");
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
