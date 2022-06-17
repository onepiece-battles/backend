package com.yichi.onepiece.Utils;

import java.util.Random;

public class RandomGenerator {
  public static int generateInteger(int minimum, int maximum) {
    Random random = new Random();
    return random.nextInt(maximum - minimum + 1) + minimum;
  }
}
