package com.yichi.onepiece.assistant.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.ibatis.annotations.Select;
import org.testng.annotations.Test;

/**
 * Runs the real V029 migration and the real {@code listByConversation} SQL
 * against H2 in MySQL mode, since MyBatis mappings are only checked at runtime.
 */
public class AiMessagePersistenceTests {
    private static final String MIGRATION = "db/migration/V000000029__add_ai_message_tool_columns.sql";

    @Test
    public void listsTheNewestMessagesOldestFirstWithTheirToolColumns() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
                var statement = connection.createStatement()) {
            // ai_message as created by V027, then the real V029 on top of it.
            statement.execute("CREATE TABLE `ai_message` (id VARCHAR(36), conversation_id VARCHAR(36), "
                    + "user_id VARCHAR(36), role VARCHAR(16), content TEXT, resources TEXT, "
                    + "creation_record_id BIGINT, modification_record_id BIGINT)");
            for (String migration : migrationStatements()) {
                statement.execute(migration);
            }
            for (int i = 1; i <= 5; i++) {
                statement.execute("INSERT INTO ai_message (id, conversation_id, role, content, creation_record_id) "
                        + "VALUES ('m" + i + "', 'conv-1', 'user', 'question " + i + "', " + i + ")");
            }
            statement.execute("UPDATE ai_message SET role = 'assistant', tool_calls = '[{\"id\":\"call_1\"}]' "
                    + "WHERE id = 'm5'");
            statement.execute("INSERT INTO ai_message (id, conversation_id, role, content, creation_record_id) "
                    + "VALUES ('other', 'conv-2', 'user', 'elsewhere', 6)");

            List<String> ids = new ArrayList<>();
            String toolCallsOfNewest = null;
            try (var query = connection.prepareStatement(listByConversationSql())) {
                query.setString(1, "conv-1");
                query.setInt(2, 3);
                try (var rows = query.executeQuery()) {
                    while (rows.next()) {
                        ids.add(rows.getString("id"));
                        toolCallsOfNewest = rows.getString("tool_calls");
                        assertThat(rows.getString("tool_call_id")).isNull();
                    }
                }
            }

            // The newest three of conversation 1, oldest first — not the first three ever written.
            assertThat(ids).containsExactly("m3", "m4", "m5");
            assertThat(toolCallsOfNewest).isEqualTo("[{\"id\":\"call_1\"}]");
        }
    }

    private static String listByConversationSql() throws NoSuchMethodException {
        Select select = AiMessageDao.class
                .getMethod("listByConversation", UUID.class, int.class)
                .getAnnotation(Select.class);
        return String.join("", select.value())
                .replace("#{conversationId}", "?")
                .replace("#{limit}", "?");
    }

    private List<String> migrationStatements() throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(MIGRATION)) {
            assertThat(input).as("migration on the classpath").isNotNull();
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(line -> !line.strip().startsWith("--"))
                    .collect(Collectors.joining("\n"));
            return Arrays.stream(sql.split(";")).map(String::strip).filter(s -> !s.isEmpty()).toList();
        }
    }
}
