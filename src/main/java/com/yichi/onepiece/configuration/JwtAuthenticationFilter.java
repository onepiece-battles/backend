package com.yichi.onepiece.configuration;

import com.yichi.onepiece.constants.JwtConstants;
import com.yichi.onepiece.iam.model.TokenDao;
import com.yichi.onepiece.utils.JwtHelper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

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
    String username = JwtHelper.extractUsername(jwt);

    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
      var hasValidToken =
          tokenDao
              .findByTokenValue(jwt)
              .map(token -> !token.getExpired() && !token.getRevoked())
              .orElse(false);

      var tokenValidForUserName = JwtHelper.isTokenValid(jwt, userDetails);
      if (hasValidToken && tokenValidForUserName) {
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }
    filterChain.doFilter(request, response);
  }
}
