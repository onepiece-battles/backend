package com.yichi.onepiece.utils;

import static com.yichi.onepiece.constants.JwtConstants.ACCESS_TOKEN_EXPIRATION_PERIOD;
import static com.yichi.onepiece.constants.JwtConstants.REFRESH_TOKEN_EXPIRATION_PERIOD;

import com.yichi.onepiece.user.model.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import org.springframework.security.core.userdetails.UserDetails;

public class JwtHelper {

  private static final String SECRET_KEY =
      "REDACTED";

  public static String generateAccessToken(UserEntity user) {
    return buildToken(user, ACCESS_TOKEN_EXPIRATION_PERIOD);
  }

  public static String generateRefreshToken(UserEntity user) {
    return buildToken(user, REFRESH_TOKEN_EXPIRATION_PERIOD);
  }

  public static boolean isTokenValid(String token, UserDetails userDetails) {
    String username = extractUsername(token);
    return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
  }

  private static boolean isTokenExpired(String token) {
    return extractClaims(token).getExpiration().before(new Date());
  }

  /**
   * Generate user access token by user detail information.
   *
   * @param user user detail information.
   * @return User access token.
   */
  private static String buildToken(UserEntity user, Integer expirationPeriod) {
    return Jwts.builder()
        .setClaims(new HashMap<>())
        .setSubject(user.getUsername())
        .setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + expirationPeriod))
        .signWith(getSignInKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  private static Key getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public static String extractUsername(String token) {
    // username is set in subject part of a claim
    return extractClaims(token).getSubject();
  }

  private static Claims extractClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getSignInKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }
}
