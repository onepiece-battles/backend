package com.yichi.onepiece.testing;

import com.yichi.infra.support.mybatis.Entity.EntityObjectFactory;
import com.yichi.infra.support.mybatis.Entity.EntityReflectorFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

public class ConcurrencyDatabase implements AutoCloseable {
  public final Connection connection;
  public final Configuration configuration;
  private final UnpooledDataSource dataSource;
  private final String mysqlAdminUrl;
  private final String schemaName;
  private final String mysqlPassword;

  public ConcurrencyDatabase() throws Exception {
    String mysqlPort = System.getenv("ONEPIECE_TEST_MYSQL_PORT");
    String url;
    if (mysqlPort == null) {
      mysqlAdminUrl = null;
      schemaName = null;
      mysqlPassword = null;
      url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;LOCK_TIMEOUT=5000";
      connection = DriverManager.getConnection(url, "sa", "");
      dataSource = new UnpooledDataSource("org.h2.Driver", url, "sa", "");
    } else {
      int port = Integer.parseInt(mysqlPort);
      if (port < 1 || port > 65535) {
        throw new IllegalArgumentException("Invalid test MySQL port");
      }
      mysqlPassword = System.getenv("ONEPIECE_TEST_MYSQL_PASSWORD");
      mysqlAdminUrl = "jdbc:mysql://127.0.0.1:" + port + "/?useSSL=false&allowPublicKeyRetrieval=true";
      schemaName = "backend_regression_" + UUID.randomUUID().toString().replace("-", "");
      try (var admin = DriverManager.getConnection(mysqlAdminUrl, "root", mysqlPassword);
          var statement = admin.createStatement()) {
        statement.execute("CREATE DATABASE " + schemaName);
      }
      url = "jdbc:mysql://127.0.0.1:" + port + "/" + schemaName + "?useSSL=false&allowPublicKeyRetrieval=true";
      connection = DriverManager.getConnection(url, "root", mysqlPassword);
      dataSource = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", url, "root", mysqlPassword);
    }
    configuration = new Configuration(new Environment("test", new SpringManagedTransactionFactory(), dataSource));
    configuration.setMapUnderscoreToCamelCase(true);
    configuration.setObjectFactory(new EntityObjectFactory());
    configuration.setReflectorFactory(new EntityReflectorFactory());
    configuration.getTypeHandlerRegistry().register("com.yichi.infra.support.mybatis.typehandler");
  }

  public void execute(String sql) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  public void migrate(String name) {
    ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/" + name));
  }

  public void mapperXml(String xml) {
    new XMLMapperBuilder(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
        configuration, "test-mapper", configuration.getSqlFragments()).parse();
  }

  public SqlSessionFactory factory() {
    return new SqlSessionFactoryBuilder().build(configuration);
  }

  public UnpooledDataSource dataSource() {
    return dataSource;
  }

  public <MapperType> MapperType mapper(Class<MapperType> mapperType) {
    return new SqlSessionTemplate(factory()).getMapper(mapperType);
  }

  public <ServiceType> ServiceType transactional(ServiceType service, Class<ServiceType> serviceType) {
    var interceptor = new TransactionInterceptor();
    interceptor.setTransactionManager(new DataSourceTransactionManager(dataSource));
    interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
    var proxy = new ProxyFactory(service);
    proxy.addAdvice(interceptor);
    return serviceType.cast(proxy.getProxy());
  }

  public static byte[] bytes(UUID value) {
    return value == null ? null : ByteBuffer.allocate(16).putLong(value.getMostSignificantBits())
        .putLong(value.getLeastSignificantBits()).array();
  }

  @Override
  public void close() throws Exception {
    try {
      connection.close();
    } finally {
      if (mysqlAdminUrl != null) {
        try (var admin = DriverManager.getConnection(mysqlAdminUrl, "root", mysqlPassword);
            var statement = admin.createStatement()) {
          statement.execute("DROP DATABASE " + schemaName);
        }
      }
    }
  }
}
