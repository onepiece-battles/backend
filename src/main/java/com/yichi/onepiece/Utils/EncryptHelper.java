package com.yichi.onepiece.Utils;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

public class EncryptHelper {
    public static String getMd5String(String code) {
        return DigestUtils.md5DigestAsHex(code.getBytes(StandardCharsets.UTF_8));
    }
}
