package com.yichi.onepiece.utils;

import com.yichi.onepiece.character.model.entity.CharacterEntity;

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

  public static int calculateInitialScore(CharacterEntity characterEntity) {
    int score =   (characterEntity.getAttack() * 10 + characterEntity.getDefense() * 10 + characterEntity.getBlood())/3
            + (characterEntity.getSpeed()-20) *  5 + (characterEntity.getIntelligence()-100) * 5;
    if (score<100) {
      return 100;
    } else if (score > 12000) {
      return 12000;
    }
    return score;
  }
}
