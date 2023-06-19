package com.yichi.onepiece.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Generated;
import javax.validation.Valid;
import javax.validation.constraints.*;

/** ListCharacterResponse */
@Generated(
    value = "org.openapitools.codegen.languages.SpringCodegen",
    date = "2023-06-20T00:07:20.673543200+08:00[Asia/Shanghai]")
public class ListCharacterResponse implements Serializable {

  private static final long serialVersionUID = 1L;

  @JsonProperty("Characters")
  @Valid
  private List<Character> characters = null;

  public ListCharacterResponse characters(List<Character> characters) {
    this.characters = characters;
    return this;
  }

  public ListCharacterResponse addCharactersItem(Character charactersItem) {
    if (this.characters == null) {
      this.characters = new ArrayList<>();
    }
    this.characters.add(charactersItem);
    return this;
  }

  /**
   * Get characters
   *
   * @return characters
   */
  @Valid
  @Schema(name = "Characters", required = false)
  public List<Character> getCharacters() {
    return characters;
  }

  public void setCharacters(List<Character> characters) {
    this.characters = characters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListCharacterResponse listCharacterResponse = (ListCharacterResponse) o;
    return Objects.equals(this.characters, listCharacterResponse.characters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(characters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ListCharacterResponse {\n");
    sb.append("    characters: ").append(toIndentedString(characters)).append("\n");
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
