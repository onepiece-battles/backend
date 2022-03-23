package com.yichi.infra.support.mybatis.dao;

import com.yichi.infra.support.Utils.JavaTypeUtils;

public class DaoUtils {
  private DaoUtils() {}

  public static final boolean isEntityDao(Class<?> daoClass) {
    return BaseDao.class.isAssignableFrom(daoClass)
        || DeletableDao.class.isAssignableFrom(daoClass);
  }

  public static final Class<?> getEntityType(Class<?> daoClass) {
    if (BaseDao.class.isAssignableFrom(daoClass)) {
      return JavaTypeUtils.getReifiedGenericType(daoClass, BaseDao.class, 0);
    } else if (DeletableDao.class.isAssignableFrom(daoClass)) {
      return JavaTypeUtils.getReifiedGenericType(daoClass, DeletableDao.class, 0);
    } else {
      throw new RuntimeException("can not get entity type from " + daoClass);
    }
  }

  public static final Class<?> getKeyType(Class<?> daoClass) {
    if (BaseDao.class.isAssignableFrom(daoClass)) {
      return JavaTypeUtils.getReifiedGenericType(daoClass, BaseDao.class, 1);
    } else if (DeletableDao.class.isAssignableFrom(daoClass)) {
      return JavaTypeUtils.getReifiedGenericType(daoClass, DeletableDao.class, 1);
    } else {
      throw new RuntimeException("can not get entity type from " + daoClass);
    }
  }
}
