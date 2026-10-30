package com.yichi.onepiece.assistant.tools;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.model.I18nText;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AssistantNameRepairTests {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String EMPTY = "{\"en-US\":null,\"zh-CN\":null}";
  private JdbcTemplate jdbc;
  private AssistantNameRepair repair;
  private int actions;

  @BeforeMethod
  public void setup() {
    jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                "jdbc:h2:mem:name_repair_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_UPPER=FALSE"));
    for (String table : new String[] {"`character`", "fruit", "`group`"})
      jdbc.execute("CREATE TABLE " + table + " (id BINARY(16) PRIMARY KEY, name VARCHAR(255) NOT NULL)");
    jdbc.execute(
        "CREATE TABLE assistant_action (code VARCHAR(10) PRIMARY KEY, operation VARCHAR(40),"
            + " payload VARCHAR(4000), status VARCHAR(16), result VARCHAR(4000))");
    repair = new AssistantNameRepair(jdbc);
  }

  /** A resource with [stored] as its name, created by a [status] assistant action. */
  private UUID created(String table, String operation, String stored, String zh, String en, String status) {
    UUID id = UUID.randomUUID();
    if (stored != null)
      jdbc.update(
          "INSERT INTO " + table + " (id, name) VALUES (?, ?)",
          ResourceActionExecutor.uuidBytes(id.toString()),
          stored);
    String name =
        "{" + (zh == null ? "" : "\"zh_CN\":\"" + zh + "\"" + (en == null ? "" : ","))
            + (en == null ? "" : "\"en_US\":\"" + en + "\"") + "}";
    jdbc.update(
        "INSERT INTO assistant_action (code, operation, payload, status, result) VALUES (?,?,?,?,?)",
        "A" + (++actions),
        operation,
        "{\"input\":{\"data\":{\"name\":" + name + "}},\"snapshots\":{}}",
        status,
        "{\"content\":\"done\",\"resources\":[{\"type\":\"x\",\"id\":\"" + id + "\",\"name\":\"\"}]}");
    return id;
  }

  private I18nText name(String table, UUID id) throws Exception {
    return JSON.readValue(
        jdbc.queryForObject(
            "SELECT name FROM " + table + " WHERE id=?",
            String.class,
            ResourceActionExecutor.uuidBytes(id.toString())),
        I18nText.class);
  }

  @Test
  public void restoresEmptyNamesFromTheConfirmedActionsOnly() throws Exception {
    UUID perona = created("`character`", "create_character", EMPTY, "佩罗娜", "Perona", "DONE");
    UUID horo = created("fruit", "create_fruit", "{}", "灵灵果实", null, "DONE");
    UUID renamed = created("`group`", "create_group", "{\"zh-CN\":\"新名字\"}", "旧名字", null, "DONE");
    UUID proposed = created("`character`", "create_character", EMPTY, "霍古巴克", null, "PREPARED");
    created("`character`", "create_character", null, "被删掉的", null, "DONE");
    UUID legacy = created("fruit", "create_fruit", "plain text", "影子果实", null, "DONE");

    assertThat(repair.repair()).isEqualTo(2);

    assertThat(name("`character`", perona)).isEqualTo(new I18nText().zhCN("佩罗娜").enUS("Perona"));
    assertThat(name("fruit", horo)).isEqualTo(new I18nText().zhCN("灵灵果实"));
    assertThat(name("`group`", renamed)).isEqualTo(new I18nText().zhCN("新名字"));
    assertThat(name("`character`", proposed)).isEqualTo(new I18nText());
    assertThat(
            jdbc.queryForObject(
                "SELECT name FROM fruit WHERE id=?",
                String.class,
                ResourceActionExecutor.uuidBytes(legacy.toString())))
        .isEqualTo("plain text");
    assertThat(repair.repair()).isZero(); // nothing left to do on the next start
  }
}
