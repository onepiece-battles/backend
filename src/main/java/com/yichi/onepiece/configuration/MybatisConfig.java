package com.yichi.onepiece.configuration;

import com.yichi.infra.support.mybatis.Entity.EntityObjectFactory;
import com.yichi.infra.support.mybatis.Entity.EntityReflectorFactory;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisConfig {
  @Bean
  ConfigurationCustomizer mybatisConfigurationCustomizer() {
    return configuration -> {
      configuration.setObjectFactory(new EntityObjectFactory());
      configuration.setReflectorFactory(new EntityReflectorFactory());
    };
  }
}
