package com.yichi.infra.support.mybatis.Entity;

import com.yichi.infra.support.entity.EntityUtil;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.Reflector;

public class EntityReflectorFactory extends DefaultReflectorFactory {
  public EntityReflectorFactory() {}

  public Reflector findForClass(Class<?> type) {
    return EntityUtil.isEntityInterface(type)
        ? super.findForClass(EntityUtil.getImplClass(type))
        : super.findForClass(type);
  }
}
