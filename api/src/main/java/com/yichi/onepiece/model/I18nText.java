package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.*;
import java.util.Objects;
import javax.annotation.Generated;
import javax.validation.constraints.*;

/** I18nText */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-19T21:18:43.064586200+08:00[Asia/Shanghai]")
public class I18nText implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("en-US")
  private String enUS;

  @JsonProperty("zh-CN")
  private String zhCN;

  public I18nText enUS(String enUS) {
    this.enUS = enUS;
    return this;
  }

  /**
   * Get enUS
   *
   * @return enUS
   */
  @Schema(name = "en-US", required = false)
  public String getEnUS() {
    return enUS;
  }

  public void setEnUS(String enUS) {
    this.enUS = enUS;
  }

  public I18nText zhCN(String zhCN) {
    this.zhCN = zhCN;
    return this;
  }

  /**
   * Get zhCN
   *
   * @return zhCN
   */
  @Schema(name = "zh-CN", required = false)
  public String getZhCN() {
    return zhCN;
  }

  public void setZhCN(String zhCN) {
    this.zhCN = zhCN;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    I18nText i18nText = (I18nText) o;
    return Objects.equals(this.enUS, i18nText.enUS) && Objects.equals(this.zhCN, i18nText.zhCN);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enUS, zhCN);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class I18nText {\n");
    sb.append("    enUS: ").append(toIndentedString(enUS)).append("\n");
    sb.append("    zhCN: ").append(toIndentedString(zhCN)).append("\n");
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
