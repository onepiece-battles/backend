package com.yichi.onepiece;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.yichi"})
@MapperScan("com.yichi.onepiece")
public class OnePieceApplication {
  @PostConstruct
  public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
  }

  public static void main(String[] args) {
    SpringApplication.run(OnePieceApplication.class, args);
  }
}
