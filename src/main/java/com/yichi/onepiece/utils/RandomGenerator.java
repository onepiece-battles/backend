package com.yichi.onepiece.utils;

import java.util.Random;

public class RandomGenerator {
  public static int generateInteger(int minimum, int maximum) {
    Random random = new Random();
    return random.nextInt(maximum - minimum + 1) + minimum;
  }

  public static String generateVerificationCode() {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < 6; i++) {
      stringBuilder.append(generateInteger(0,9));
    }
    return stringBuilder.toString();
  }
}
