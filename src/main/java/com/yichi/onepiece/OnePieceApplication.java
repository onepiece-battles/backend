package com.yichi.onepiece;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.yichi"})
@MapperScan(value = "com.yichi")
public class OnePieceApplication {
  public static void main(String[] args) {
    SpringApplication.run(OnePieceApplication.class, args);
  }
}
