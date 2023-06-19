package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.*;
import java.util.Objects;
import javax.annotation.Generated;
import javax.validation.constraints.*;

/** ListCharacterRequest */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-19T21:18:43.064586200+08:00[Asia/Shanghai]")
public class ListCharacterRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("page_size")
  private Integer pageSize;

  @JsonProperty("page_index")
  private Integer pageIndex;

  public ListCharacterRequest pageSize(Integer pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  /**
   * Get pageSize minimum: 0
   *
   * @return pageSize
   */
  @NotNull
  @Min(0)
  @Schema(name = "page_size", required = true)
  public Integer getPageSize() {
    return pageSize;
  }

  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public ListCharacterRequest pageIndex(Integer pageIndex) {
    this.pageIndex = pageIndex;
    return this;
  }

  /**
   * Get pageIndex minimum: 1
   *
   * @return pageIndex
   */
  @NotNull
  @Min(1)
  @Schema(name = "page_index", required = true)
  public Integer getPageIndex() {
    return pageIndex;
  }

  public void setPageIndex(Integer pageIndex) {
    this.pageIndex = pageIndex;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListCharacterRequest listCharacterRequest = (ListCharacterRequest) o;
    return Objects.equals(this.pageSize, listCharacterRequest.pageSize)
        && Objects.equals(this.pageIndex, listCharacterRequest.pageIndex);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pageSize, pageIndex);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ListCharacterRequest {\n");
    sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
    sb.append("    pageIndex: ").append(toIndentedString(pageIndex)).append("\n");
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
