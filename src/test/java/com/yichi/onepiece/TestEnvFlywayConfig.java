package com.yichi.onepiece;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestEnvFlywayConfig {
  private final Logger logger = LoggerFactory.getLogger(TestEnvFlywayConfig.class);

  @Autowired DataSource dataSource;

  @PostConstruct
  public void migrate() {
    FluentConfiguration fluentConfiguration =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("db/migration")
            .encoding("utf-8")
            .validateOnMigrate(true)
            .baselineOnMigrate(true);
    var flyway = fluentConfiguration.load();
    try {
      flyway.repair();
      flyway.migrate();
    } catch (FlywayException e) {
      e.printStackTrace();
      logger.error("error when migrating schema");
    }
  }
}
