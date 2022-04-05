package com.yichi.infra.support.utils;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public class UUIDUtils {
  private UUIDUtils() {}

  public static String encode(UUID uuid) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(toBytes(uuid));
  }

  public static UUID decode(String uuid) {
    byte[] bytes = Base64.getUrlDecoder().decode(uuid);
    byte[] bytes16;
    if (bytes.length < 16) {
      bytes16 = new byte[16];
      System.arraycopy(bytes, 0, bytes16, 16 - bytes.length, bytes.length);
      return fromBytes(bytes16);
    } else if (bytes.length > 16) {
      bytes16 = new byte[16];
      System.arraycopy(bytes, bytes.length - 16, bytes16, 0, 16);
      return fromBytes(bytes16);
    } else {
      return fromBytes(bytes);
    }
  }

  public static byte[] toBytes(UUID uuid) {
    return ByteBuffer.allocate(16)
        .putLong(uuid.getMostSignificantBits())
        .putLong(uuid.getLeastSignificantBits())
        .array();
  }

  public static UUID fromBytes(byte[] bytes) {
    ByteBuffer bb = ByteBuffer.wrap(bytes);
    long high = bb.getLong();
    long low = bb.getLong();
    return new UUID(high, low);
  }
}
