package com.yichi.onepiece.Character.Service;

import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class CharacterService {

  public static void main(String[] args) {
    String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    Random random = new Random();
    for (int k = 0; k < 10; k++) {
      StringBuffer buffer = new StringBuffer();
      for (int i = 0; i < 8; i++) {
        int number = random.nextInt(62);
        buffer.append(str.charAt(number));
      }
      System.out.println(buffer.toString());
    }
  }
}
