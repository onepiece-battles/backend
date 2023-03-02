package com.yichi.infra.support.mybatis.Entity;

import com.yichi.infra.support.entity.EntityUtil;
import com.yichi.infra.support.mybatis.result.RowList;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;

public class EntityObjectFactory extends DefaultObjectFactory {
  private static final long serialVersionUID = 1L;

  public EntityObjectFactory() {}

  protected Class<?> resolveInterface(Class<?> type) {
    return EntityUtil.isEntityInterface(type)
        ? EntityUtil.getImplClass(type)
        : super.resolveInterface(type);
  }

  public <T> boolean isCollection(Class<T> type) {
    return !RowList.class.equals(type) && super.isCollection(type);
  }
}
