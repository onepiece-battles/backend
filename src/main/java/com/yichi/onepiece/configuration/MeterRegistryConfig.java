package com.yichi.onepiece.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeterRegistryConfig {
  @Bean
  public MeterRegistry getSimpleMeterRegistry() {
    return new SimpleMeterRegistry();
  }
}
