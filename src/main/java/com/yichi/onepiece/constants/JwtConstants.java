package com.yichi.onepiece.constants;

public class JwtConstants {
  public static final int ACCESS_TOKEN_EXPIRATION_PERIOD = 1000 * 60 * 24;
  public static final int REFRESH_TOKEN_EXPIRATION_PERIOD = ACCESS_TOKEN_EXPIRATION_PERIOD * 10;

  public static final String HTTP_HEADER_AUTH = "Authorization";
  public static final String HTTP_HEADER_AUTH_SCHEME = "Bearer ";
}
