package com.yichi.infra.support.entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
  String builder() default "com.yichi.infra.support.EntityBuilder";

  String[] deprecatedFields() default {};
}
