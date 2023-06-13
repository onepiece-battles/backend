package com.yichi.onepiece;

import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@TestConfiguration
public class TestConfigure {

  @Autowired Environment environment;

  // define data source
  @Bean
  @Primary
  @Profile("test-h2")
  DataSource dataSource() {
    // Because Dao tests are running in parallel, there are some race conditions if all Dao tests
    // are
    // connecting the same database. The following bean override the default setting and connect to
    // different database for each thread to solve the race conditions.
    String databaseName = UUID.randomUUID().toString().substring(0, 8);
    String dbUrl =
        String.format(
            "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;MODE=MySQL", databaseName);
    DriverManagerDataSource dataSource = new DriverManagerDataSource();
    dataSource.setDriverClassName("org.h2.Driver");
    dataSource.setUrl(dbUrl);
    dataSource.setUsername("nika");
    dataSource.setPassword("nika");
    System.out.println("load data base finish");
    return dataSource;
  }
}
