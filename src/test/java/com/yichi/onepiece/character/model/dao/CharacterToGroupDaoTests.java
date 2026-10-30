package com.yichi.onepiece.character.model.dao;

import static org.assertj.core.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.apache.ibatis.annotations.Select;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testng.annotations.Test;

public class CharacterToGroupDaoTests {
  private static byte[] bytes(UUID id) {
    return ByteBuffer.allocate(16)
        .putLong(id.getMostSignificantBits())
        .putLong(id.getLeastSignificantBits())
        .array();
  }

  /** The group page and the assistant read a member's characterId; it used to come back null. */
  @Test
  public void groupMembersCarryTheirCharacterAndGroupIds() throws Exception {
    var jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                "jdbc:h2:mem:members_" + UUID.randomUUID()
                    + ";DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_UPPER=FALSE"));
    jdbc.execute(
        "CREATE TABLE `character` (id BINARY(16) PRIMARY KEY, name VARCHAR(255), image_uri"
            + " VARCHAR(255), system_resource BOOLEAN)");
    jdbc.execute(
        "CREATE TABLE character_to_group (id BINARY(16) PRIMARY KEY, character_id BINARY(16),"
            + " group_id BINARY(16), position VARCHAR(32), once_belonged_to BOOLEAN)");
    UUID character = UUID.randomUUID(), group = UUID.randomUUID(), membership = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO `character` VALUES (?, '{}', null, true)", (Object) bytes(character));
    jdbc.update(
        "INSERT INTO character_to_group VALUES (?, ?, ?, 'member', false)",
        bytes(membership),
        bytes(character),
        bytes(group));
    String sql =
        String.join(
                "",
                CharacterToGroupDao.class
                    .getMethod("listCharacterInGroupViewByGroupId", UUID.class)
                    .getAnnotation(Select.class)
                    .value())
            .replace("#{groupId}", "?");

    var row = jdbc.queryForMap(sql, (Object) bytes(group));

    assertThat((byte[]) row.get("character_id")).isEqualTo(bytes(character));
    assertThat((byte[]) row.get("group_id")).isEqualTo(bytes(group));
    assertThat((byte[]) row.get("id")).isEqualTo(bytes(character)); // still the character's id
  }
}
