package com.yichi.onepiece.Application;

import static org.assertj.core.api.Assertions.assertThat;

import com.yichi.onepiece.TestUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.annotations.Test;

@SpringBootTest
@TestPropertySource(locations = "classpath:test-h2.properties")
public class DatabaseSchemaTests extends AbstractTransactionalTestNGSpringContextTests {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseSchemaTests.class);
  private static final String COMMENTS = "-- THIS IS THE DB GOLDEN SCHEMA FOR UNIT TEST.";
  private static final String MARKDOWN_TITLE = "# DB GOLDEN SCHEMA";
  private static final String RESOURCE_PATH = "src/main/resources";
  private static final String GOLDEN_SCHEMA = "db/schema.sql";
  private static final String MARKDOWN_SCHEMA = "db/schema.md";
  private static final String[] DB_MIGRATION_DIRs = {"db/migration"};
  private static final String LATEST_VERSION = "db/LATEST_VERSION";
  // matches the file name "V00000001__xxx.sql"
  private static final Pattern MIGRATION_SQL_NAME_PATTERN =
      Pattern.compile("\\bV([0-9]+)__\\w+\\.sql\\b");
  private static final String DB_SCHEMA = "PUBLIC";

  @Autowired private SchemaMapper schemaMapper;

  @Test
  public void validateSchema() throws IOException {
    String goldenSchema = readGoldenSchema();
    String sqlSchema = generateSqlSchema();
    if (!sqlSchema.equals(goldenSchema)) {
      Files.writeString(Path.of(RESOURCE_PATH, GOLDEN_SCHEMA), sqlSchema);
      Files.writeString(Path.of(RESOURCE_PATH, MARKDOWN_SCHEMA), generateMarkdownSchema());
    }
    assertThat(sqlSchema)
        .as("Schema does not match the golden.")
        .isEqualToNormalizingWhitespace(goldenSchema);
  }

  @Test
  public void validateMigrationVersion() throws IOException {
    String content = TestUtils.loadResourceFile(LATEST_VERSION);
    logger.info("Latest version:\n{}", content);

    // Check the latest version is in the file
    Matcher scriptNameMatcher = MIGRATION_SQL_NAME_PATTERN.matcher(content);
    assertThat(scriptNameMatcher.find())
        .as("Can not find the latest version in LATEST_VERSION")
        .isTrue();
    final int latestVersion = Integer.parseInt(scriptNameMatcher.group(1));

    // Check there is a migration script for each version.
    var migrationPaths =
        Arrays.stream(DB_MIGRATION_DIRs)
            .map(dir -> new File(TestUtils.getResourceFilePath(dir)))
            .collect(Collectors.toList());
    List<File> migrationScripts = new ArrayList<>();
    for (var dir : migrationPaths) {
      // Skip Repeatable migrations.
      var scripts = dir.listFiles(file -> file.getName().startsWith("V"));
      if (scripts != null) {
        migrationScripts.addAll(Arrays.asList(scripts));
      }
    }
    assertThat(migrationScripts).hasSize(latestVersion);

    // Check the latest version is really the "latest"
    Set<Integer> versions = new HashSet<>();
    for (var file : migrationScripts) {
      String fileName = file.getName();
      logger.info("{}", fileName);

      var matcher = MIGRATION_SQL_NAME_PATTERN.matcher(fileName);
      assertThat(matcher.find())
          .as(
              "Migration script: \"%s\" does not match the pattern \"%s\"",
              fileName, MIGRATION_SQL_NAME_PATTERN.toString())
          .isTrue();
      int version = Integer.parseInt(matcher.group(1));
      assertThat(version)
          .as("The latest version in LATEST_VERSION is not up-to-date")
          .isLessThanOrEqualTo(latestVersion);

      assertThat(versions.add(version))
          .as("Duplicate migration version found: %s", fileName)
          .isTrue();

      // Check migration sql
      String migrationContent = Files.readString(file.toPath());
      verifyMigrationContent(migrationContent);
    }
  }

  private String generateSqlSchema() {
    String tableContent =
        schemaMapper.showTables(DB_SCHEMA).stream()
            .filter(table -> !"flyway_schema_history".equals(table.getTableName()))
            .map(
                table ->
                    String.format(
                        "-- %s\n%s;\n%s",
                        table.getTableName(),
                        normalizeTableSql(table.getSql()),
                        describeTable(table.getTableName())))
            .collect(Collectors.joining("\n"));

    return String.format("%s\n\n%s", COMMENTS, tableContent);
  }

  private String describeTable(String tableName) {
    String constraints =
        schemaMapper.showConstraints(DB_SCHEMA, tableName).stream()
            .map(c -> String.format("%s;\n", normalizeConstraintSql(c)))
            .collect(Collectors.joining(""));

    String indexes =
        schemaMapper.showIndexes(DB_SCHEMA, tableName).stream()
            .filter(index -> index.getConstraintName() == null)
            .map(index -> String.format("%s;\n", removeSchema(index.getSql())))
            .sorted()
            .distinct()
            .collect(Collectors.joining(""));

    return replaceQuote(constraints + indexes);
  }

  private String normalizeTableSql(String sql) {
    return replaceQuote(removeSchema(sql))
        // `NEXT VALUE FOR` stands for AUTO INCREMENT in H2.
        .replaceAll("DEFAULT NEXT VALUE FOR[^,]+", "AUTO_INCREMENT")
        // Replace `CREATE MEMORY TABLE` with `CREATE TABLE`.
        .replaceAll("CREATE\\s+MEMORY\\s+TABLE", "CREATE TABLE")
        // Remove `FORCE` and column list.
        .replaceAll("CREATE\\sFORCE\\sVIEW\\s(\\S+)\\([^)]+\\)", "CREATE VIEW $1")
        // Remove SELECTIVITY
        .replaceAll("SELECTIVITY\\s+\\d+\\s+", "");
  }

  private String normalizeConstraintSql(String sql) {
    return removeSchema(sql)
        // Remove Index associated with constraint.
        .replaceAll("\\s?INDEX\\s+\"[\\w\\d_]+\"", "")
        // Remove PK name as it is not persistent. PK Column name is not affected.
        .replaceAll("CONSTRAINT\\s+\"[\\w\\d_]+\"\\s+PRIMARY\\s+KEY", "CONSTRAINT PRIMARY KEY")
        // Break line before Foreign Key.
        .replaceAll("\\s+FOREIGN\\s+KEY", "\n  FOREIGN KEY")
        // Break line before UNIQUE
        .replaceAll("\\s+UNIQUE\\(", "\n  UNIQUE(")
        // Remove `NOCHECK`.
        .replaceAll("\\s+NOCHECK", "");
  }

  private String removeSchema(String sql) {
    // Remove db schema `PUBLIC.` as it is redundant.
    return sql.replaceAll(String.format("\"%s\"\\.", DB_SCHEMA), "");
  }

  private String replaceQuote(String sql) {
    // Replace " around field name with ` as it is not supported in MySQL.
    return sql.replace("\"", "`");
  }

  private String generateMarkdownSchema() {
    String tableContent =
        schemaMapper.showTables(DB_SCHEMA).stream()
            .filter(table -> !"flyway_schema_history".equals(table.getTableName()))
            .map(
                table ->
                    String.format(
                        "## %s\n\n%s", table.getTableName(), describeTableAsMarkdown(table)))
            .collect(Collectors.joining("\n"));

    return String.format("%s\n\n%s", MARKDOWN_TITLE, tableContent);
  }

  private String describeTableAsMarkdown(TableEntity table) {
    String tableSql = String.format("```sql\n%s;\n```\n", normalizeTableSql(table.getSql()));
    Stream<String> constraints =
        schemaMapper.showConstraints(DB_SCHEMA, table.getTableName()).stream()
            .map(c -> String.format("```sql\n%s;\n```\n", normalizeConstraintSql(c)))
            .map(this::replaceQuote);

    Stream<String> indexes =
        schemaMapper.showIndexes(DB_SCHEMA, table.getTableName()).stream()
            .filter(index -> index.getConstraintName() == null)
            .map(index -> String.format("```sql\n%s;\n```\n", removeSchema(index.getSql())))
            .sorted()
            .distinct()
            .map(this::replaceQuote);

    return Stream.of(Stream.of(tableSql), constraints, indexes)
        .flatMap(Function.identity())
        .collect(Collectors.joining("\n"));
  }

  private void verifyMigrationContent(String migrationScript) {
    assertThat(migrationScript)
        .as("Double Quote (\") not allowed in migration script.")
        .doesNotContain("\"") // Disable "
        .as("Invalid comment")
        .doesNotMatch(Pattern.compile(".*--\\S.*", Pattern.DOTALL)) // Check invalid comment
        // Check DROP CONSTRAINT
        .as("DROP CONSTRAINT is not supported")
        .doesNotMatch(
            Pattern.compile(".*DROP\\s+CONSTRAINT.*", Pattern.CASE_INSENSITIVE + Pattern.DOTALL))
        //      Check ALTER TABLE xxx RENAME COLUMN xxx TO
        .as("RENAME COLUMN TO is not supported")
        .doesNotMatch(
            Pattern.compile(".*RENAME\\s+COLUMN.*TO.*", Pattern.CASE_INSENSITIVE + Pattern.DOTALL));
  }

  private String readGoldenSchema() throws IOException {
    return TestUtils.loadResourceFile(GOLDEN_SCHEMA);
  }
}
