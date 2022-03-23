package com.yichi.infra.support.entity;

public interface EntityImpl {
  Object get(String var1) throws NoSuchFieldException;

  void set(String var1, Object var2) throws NoSuchFieldException;
}
