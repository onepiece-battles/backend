package com.yichi.onepiece.configuration;

import com.yichi.onepiece.user.model.UserDao;
import com.yichi.onepiece.user.model.UserDetailDao;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class ApplicationConfig {
  private final UserDao userDao;

  public ApplicationConfig(UserDao userDao) {
    this.userDao = userDao;
  }

  @Bean
  @Qualifier("onepieceClock")
  public Clock onepieceClock() {
    return Clock.systemUTC();
  }

  /*
     Configure the implementation of UserDetailService
  */
  @Bean
  public UserDetailsService getUserDetailService() {
    return username ->
            userDao
            .getUserDetailByName(username)
            .orElseThrow(() -> new UsernameNotFoundException("User detail not found"));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  // customize authentication provider
  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
    authenticationProvider.setUserDetailsService(getUserDetailService());
    authenticationProvider.setPasswordEncoder(passwordEncoder());
    return authenticationProvider;
  }
}
