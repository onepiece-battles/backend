package com.yichi.infra.support.entity;

import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;

public class EntityTypeResolver extends SimpleAbstractTypeResolver {
  private static final long serialVersionUID = 1L;

  public EntityTypeResolver() {}

  public JavaType findTypeMapping(DeserializationConfig config, JavaType type) {
    Class<?> src = type.getRawClass();
    return EntityUtil.isEntityInterface(src)
        ? config.getTypeFactory().constructSpecializedType(type, EntityUtil.getImplClass(src))
        : null;
  }
}
