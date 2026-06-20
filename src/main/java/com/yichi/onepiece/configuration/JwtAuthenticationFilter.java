package com.yichi.onepiece.configuration;

import com.yichi.onepiece.constants.JwtConstants;
import com.yichi.onepiece.iam.model.TokenDao;
import com.yichi.onepiece.utils.JwtHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final TokenDao tokenDao;

  private final UserDetailsService userDetailsService;

  public JwtAuthenticationFilter(TokenDao tokenDao, UserDetailsService userDetailsService) {
    this.tokenDao = tokenDao;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String authentication = request.getHeader(JwtConstants.HTTP_HEADER_AUTH);
    if (authentication == null
        || !authentication.startsWith(JwtConstants.HTTP_HEADER_AUTH_SCHEME)) {
      filterChain.doFilter(request, response);
      return;
    }
    // get read jwt by removing the prefix
    String jwt = authentication.substring(JwtConstants.HTTP_HEADER_AUTH_SCHEME.length());
    String username;
    try {
      username = JwtHelper.extractUsername(jwt);
    } catch (IllegalArgumentException | JwtException e) {
      logger.warn("Invalid JWT received", e);
      filterChain.doFilter(request, response);
      return;
    }

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
      var hasValidToken =
          tokenDao
              .findByTokenValue(jwt)
              .map(token -> !token.getExpired() && !token.getRevoked())
              .orElse(false);

      logger.info("valid token is: {}", hasValidToken);

      var tokenValidForUserName = JwtHelper.isTokenValid(jwt, userDetails);
      if (hasValidToken && tokenValidForUserName) {
        logger.info("tokenValidForUserName");
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        // Create new context instead of using "SecurityContextHolder.getContext()" to avoid race
        // condition.
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        context.setAuthentication(authToken);
        SecurityContextHolder.setContext(context);
      }
    }
    filterChain.doFilter(request, response);
  }
}
