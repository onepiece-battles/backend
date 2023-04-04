package com.yichi.onepiece.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.yichi.onepiece.model.Fruit;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.io.Serializable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * ListFruitResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-09T16:50:20.075306800+08:00[Asia/Shanghai]")
public class ListFruitResponse  implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("fruits")
  @Valid
  private List<Fruit> fruits = null;

  public ListFruitResponse fruits(List<Fruit> fruits) {
    this.fruits = fruits;
    return this;
  }

  public ListFruitResponse addFruitsItem(Fruit fruitsItem) {
    if (this.fruits == null) {
      this.fruits = new ArrayList<>();
    }
    this.fruits.add(fruitsItem);
    return this;
  }

  /**
   * Get fruits
   * @return fruits
  */
  @Valid 
  @Schema(name = "fruits", required = false)
  public List<Fruit> getFruits() {
    return fruits;
  }

  public void setFruits(List<Fruit> fruits) {
    this.fruits = fruits;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListFruitResponse listFruitResponse = (ListFruitResponse) o;
    return Objects.equals(this.fruits, listFruitResponse.fruits);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fruits);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ListFruitResponse {\n");
    sb.append("    fruits: ").append(toIndentedString(fruits)).append("\n");
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

