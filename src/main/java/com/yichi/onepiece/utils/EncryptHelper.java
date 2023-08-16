package com.yichi.onepiece.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.util.DigestUtils;

public class EncryptHelper {
  public static String getMd5String(String code) {
    return DigestUtils.md5DigestAsHex(code.getBytes(StandardCharsets.UTF_8));
  }

  public static String base64Encode(byte[] input) {
    return Base64.getEncoder().encodeToString(input);
  }

  public static byte[] base64Decode(String input) {
    return Base64.getDecoder().decode(input);
  }
}
