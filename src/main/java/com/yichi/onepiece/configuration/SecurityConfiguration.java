package com.yichi.onepiece.configuration;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final AuthenticationProvider authenticationProvider;

  public SecurityConfiguration(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      AuthenticationProvider authenticationProvider) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.authenticationProvider = authenticationProvider;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
    httpSecurity
        // this configuration is mainly for local env test, because the frontend is started on
        // localhost:8081, thus need to tell spring security to allow request(with authentication)
        // from
        // http://127.0.0.1:8081 to pass.
        .cors(httpSecurityCorsConfigurer -> httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            authorize -> {
              authorize
                  .requestMatchers(HttpMethod.OPTIONS, "/**")
                  .permitAll()
                  .requestMatchers("/api/onepiece/user/login", "/api/onepiece/user/register", "/chat")
                  .permitAll()
                  .requestMatchers(
                      "/api/onepiece/email/send_password_retrieval_email",
                      "/api/onepiece/user/update_password_with_verification")
                  .permitAll()
                  .anyRequest()
                  .authenticated();
            })
        // specify authentication provider
        .authenticationProvider(authenticationProvider)
        // specify authentication
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return httpSecurity.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    Arrays.stream(getAllowedOrigins()).forEach(configuration::addAllowedOriginPattern);
    configuration.addAllowedMethod("*");
    configuration.addAllowedHeader("*");
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  private String[] getAllowedOrigins() {
    String origins = System.getenv("ONEPIECE_ALLOWED_ORIGINS");
    if (origins == null || origins.isBlank()) {
      origins = "*";
    }
    return Arrays.stream(origins.split(",")).map(String::trim).filter(origin -> !origin.isEmpty()).toArray(String[]::new);
  }
}
