package com.yichi.onepiece.assistant.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.model.I18nText;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Restores the names of resources the assistant created before onepiece-battles/backend#27: their
 * confirmed create actions saved an empty name, because the action schema's zh_CN / en_US keys were
 * not read into I18nText. The intended name is still in each action's payload. Runs at startup and
 * only touches a resource whose name is still empty, so it is safe to run again and does nothing
 * once every name is back.
 */
@Component
public class AssistantNameRepair implements ApplicationRunner {
  private static final Logger logger = LoggerFactory.getLogger(AssistantNameRepair.class);
  private static final Map<String, String> TABLES =
      Map.of(
          "create_character", "`character`",
          "create_fruit", "fruit",
          "create_group", "`group`");
  private static final ObjectMapper JSON =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final JdbcTemplate jdbc;

  public AssistantNameRepair(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      int restored = repair();
      if (restored > 0) logger.info("restored the names of {} assistant-created resources", restored);
    } catch (DataAccessException e) {
      logger.warn("could not restore the names of assistant-created resources", e);
    }
  }

  /** The number of names restored. */
  int repair() {
    int restored = 0;
    var actions =
        jdbc.queryForList(
            "SELECT operation, payload, result FROM assistant_action WHERE status='DONE' AND"
                + " operation IN ('create_character','create_fruit','create_group')");
    for (var action : actions) {
      String table = TABLES.get(String.valueOf(action.get("operation")));
      try {
        JsonNode payload = JSON.readTree(String.valueOf(action.get("payload")));
        JsonNode result = JSON.readTree(String.valueOf(action.get("result")));
        var name =
            ActionSchemas.i18nText(payload.path("input").path("data").path("name"), new I18nText());
        if (blank(name)) continue;
        byte[] id =
            ResourceActionExecutor.uuidBytes(result.path("resources").path(0).path("id").asText());
        var stored = jdbc.queryForList("SELECT name FROM " + table + " WHERE id=?", String.class, id);
        // A deleted resource, or a name that is not I18nText JSON, is left alone.
        if (stored.isEmpty() || !blank(JSON.readValue(stored.get(0), I18nText.class))) continue;
        restored +=
            jdbc.update(
                "UPDATE " + table + " SET name=? WHERE id=?", JSON.writeValueAsString(name), id);
      } catch (JsonProcessingException | IllegalArgumentException e) {
        logger.debug("skipped an assistant action whose payload or result could not be read", e);
      }
    }
    return restored;
  }

  private static boolean blank(I18nText name) {
    return name == null
        || ((name.getZhCN() == null || name.getZhCN().isBlank())
            && (name.getEnUS() == null || name.getEnUS().isBlank()));
  }
}
