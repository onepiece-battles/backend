package com.yichi.onepiece.configuration;

import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
  /*
   1. This cors configuration is for the case Spring Security is not involved.
   2. This configuration will work when request is from login and register apis.
  */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**").allowedOriginPatterns(getAllowedOrigins()).allowedMethods("*").allowedHeaders("*").allowCredentials(true);
  }

  private String[] getAllowedOrigins() {
    String origins = System.getenv("ONEPIECE_ALLOWED_ORIGINS");
    if (origins == null || origins.isBlank()) {
      origins = "*";
    }
    return Arrays.stream(origins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
  }
}
