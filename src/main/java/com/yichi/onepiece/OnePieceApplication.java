package com.yichi.onepiece;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.yichi"})
// Only @Mapper interfaces: without the filter every interface under the
// package (entities, views, service interfaces) became a mapper bean.
@MapperScan(basePackages = "com.yichi.onepiece", annotationClass = Mapper.class)
public class OnePieceApplication {
  @PostConstruct
  public void init() {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
  }

  public static void main(String[] args) {
    SpringApplication.run(OnePieceApplication.class, args);
  }
}
