package com.yichi.onepiece.assistant.tools;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.Test;

public class ActionSchemasTests {
  private static String range(String resource, String field) {
    JsonNode schema = ActionSchemas.fields(resource, true).path("properties").path(field);
    return schema.path("minimum").asLong() + "–" + schema.path("maximum").asLong();
  }

  @Test
  public void rangesMatchTheAppsCreateForms() {
    // The manual create forms: energy 100–300, bounty up to 100 billion, a fruit's energy cost 1–10.
    assertThat(range("character", "energy")).isEqualTo("100–300");
    assertThat(range("character", "bounty")).isEqualTo("10–100000000000");
    assertThat(range("character", "attack")).isEqualTo("10–1500");
    assertThat(range("fruit", "energy_consume")).isEqualTo("1–10");
    assertThat(range("fruit", "blood_intercept")).isEqualTo("-1000–3000");
  }
}
