package com.yichi.infra.support.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.ObjectTypeHandler;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Column {
  boolean json() default false;

  boolean ignoreUpdates() default false;

  Class<? extends BaseTypeHandler<?>> typeHandler() default ObjectTypeHandler.class;
}
