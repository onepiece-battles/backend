package com.yichi.onepiece;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.yichi"})
public class OnePieceApplication {
  public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
  }

  public static void main(String[] args) {
    SpringApplication.run(OnePieceApplication.class, args);
  }
}
