package com.yichi.onepiece.Utils;

import java.nio.charset.StandardCharsets;
import org.springframework.util.DigestUtils;

public class EncryptHelper {
  public static String getMd5String(String code) {
    return DigestUtils.md5DigestAsHex(code.getBytes(StandardCharsets.UTF_8));
  }
}
