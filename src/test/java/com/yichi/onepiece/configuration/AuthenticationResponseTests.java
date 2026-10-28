package com.yichi.onepiece.configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yichi.onepiece.iam.model.TokenDao;
import com.yichi.onepiece.iam.model.TokenEntity;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.utils.JwtHelper;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.Filter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.testng.annotations.Test;

public class AuthenticationResponseTests {
  @Test
  public void internalErrorsPreserveTheirStatusWithoutOpeningTheErrorUrl() throws Exception {
    try (var context = new AnnotationConfigWebApplicationContext()) {
      context.setServletContext(new MockServletContext());
      context.register(TestConfiguration.class);
      context.refresh();
      var mvc = MockMvcBuilders.webAppContextSetup(context)
          .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
      for (int errorStatus : new int[] {404, 500}) {
        mvc.perform(post("/error").with(request -> {
              request.setDispatcherType(DispatcherType.ERROR);
              return request;
            })
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, errorStatus))
            .andExpect(status().is(errorStatus));
      }
      mvc.perform(post("/error")).andExpect(status().isUnauthorized());
      mvc.perform(post("/api/onepiece/user/list")).andExpect(status().isUnauthorized());
    }
  }

  @Test
  public void protectedResourcesReturn401ForMissingOrInvalidToken() throws Exception {
    try (var context = new AnnotationConfigWebApplicationContext();
        var jwt = mockStatic(JwtHelper.class)) {
      jwt.when(() -> JwtHelper.extractUsername("invalid-token"))
          .thenThrow(new MalformedJwtException("invalid test token"));
      jwt.when(() -> JwtHelper.extractUsername("expired-token"))
          .thenThrow(new ExpiredJwtException(null, null, "expired test token"));
      jwt.when(() -> JwtHelper.extractUsername("valid-token")).thenReturn("tester");
      jwt.when(() -> JwtHelper.extractUsername("revoked-token")).thenReturn("tester");
      jwt.when(() -> JwtHelper.isTokenValid(eq("valid-token"), any(UserDetails.class)))
          .thenReturn(true);
      jwt.when(() -> JwtHelper.isTokenValid(eq("revoked-token"), any(UserDetails.class)))
          .thenReturn(true);
      context.setServletContext(new MockServletContext());
      context.register(TestConfiguration.class);
      context.refresh();
      var mvc = MockMvcBuilders.webAppContextSetup(context)
          .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
      mvc.perform(post("/api/onepiece/character/list_characters"))
          .andExpect(status().isUnauthorized());
      mvc.perform(post("/api/onepiece/battle/get_battle_full_view")
              .header("Authorization", "Bearer invalid-token"))
          .andExpect(status().isUnauthorized());
      mvc.perform(post("/api/onepiece/character/list_characters")
              .header("Authorization", "Bearer revoked-token"))
          .andExpect(status().isUnauthorized());
      mvc.perform(post("/api/onepiece/character/list_characters")
              .header("Authorization", "Bearer expired-token"))
          .andExpect(status().isUnauthorized());
      mvc.perform(post("/api/onepiece/denied")
              .header("Authorization", "Bearer valid-token"))
          .andExpect(status().isForbidden());
      mvc.perform(post("/api/onepiece/user/login"))
          .andExpect(status().isOk());
    }
  }

  @Configuration
  @EnableWebMvc
  @Import(SecurityConfiguration.class)
  static class TestConfiguration {
    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter() {
      var tokens = mock(TokenDao.class);
      var valid = mock(TokenEntity.class);
      when(valid.getExpired()).thenReturn(false);
      when(valid.getRevoked()).thenReturn(false);
      when(tokens.findByTokenValue("valid-token")).thenReturn(Optional.of(valid));
      var revoked = mock(TokenEntity.class);
      when(revoked.getExpired()).thenReturn(false);
      when(revoked.getRevoked()).thenReturn(true);
      when(tokens.findByTokenValue("revoked-token")).thenReturn(Optional.of(revoked));
      UserDetailsService users = username -> User.withUsername(username)
          .password("unused").roles("NORMAL").build();
      return new JwtAuthenticationFilter(tokens, users);
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
      return mock(AuthenticationProvider.class);
    }

    @Bean
    LoginEndpoint loginEndpoint() {
      return new LoginEndpoint();
    }
  }

  @RestController
  static class LoginEndpoint {
    @RequestMapping("/error")
    ResponseEntity<Void> error(HttpServletRequest request) {
      return ResponseEntity.status((int) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).build();
    }

    @PostMapping("/api/onepiece/denied")
    String denied() {
      throw new PermissionDeniedException("Access Denied");
    }

    @PostMapping("/api/onepiece/user/login")
    String login() {
      return "ok";
    }
  }
}
