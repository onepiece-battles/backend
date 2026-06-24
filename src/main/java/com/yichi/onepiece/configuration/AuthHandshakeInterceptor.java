package com.yichi.onepiece.configuration;

import com.yichi.onepiece.constants.JwtConstants;
import com.yichi.onepiece.iam.model.TokenDao;
import com.yichi.onepiece.user.model.UserDetailImpl;
import com.yichi.onepiece.utils.JwtHelper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {
    private final TokenDao tokenDao;
    private final UserDetailsService userDetailsService;

    public AuthHandshakeInterceptor(TokenDao tokenDao, UserDetailsService userDetailsService) {
        this.tokenDao = tokenDao;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            org.springframework.http.server.ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletReq) {
            HttpServletRequest httpServletRequest = servletReq.getServletRequest();
            String jwt = getJwt(httpServletRequest);
            if (jwt == null) {
                return useLegacyUserId(httpServletRequest, attributes);
            }
            try {
                String username = JwtHelper.extractUsername(jwt);
                var userDetails = userDetailsService.loadUserByUsername(username);
                var hasValidToken =
                        tokenDao.findByTokenValue(jwt)
                                .map(token -> !token.getExpired() && !token.getRevoked())
                                .orElse(false);
                if (hasValidToken
                        && JwtHelper.isTokenValid(jwt, userDetails)
                        && userDetails instanceof UserDetailImpl userDetail) {
                    attributes.put("userId", userDetail.getId().toString());
                    return true;
                }
            } catch (IllegalArgumentException | JwtException e) {
                return false;
            }
        }
        return false;
    }

    private boolean useLegacyUserId(HttpServletRequest request, Map<String, Object> attributes) {
        String userId = request.getParameter("userId");
        if (userId == null || userId.isBlank()) {
            return false;
        }
        try {
            attributes.put("userId", UUID.fromString(userId).toString());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String getJwt(HttpServletRequest request) {
        String authorization = request.getHeader(JwtConstants.HTTP_HEADER_AUTH);
        if (authorization != null && authorization.startsWith(JwtConstants.HTTP_HEADER_AUTH_SCHEME)) {
            return authorization.substring(JwtConstants.HTTP_HEADER_AUTH_SCHEME.length());
        }
        String token = request.getParameter("token");
        return token == null || token.isBlank() ? null : token;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            org.springframework.http.server.ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
    }
}
