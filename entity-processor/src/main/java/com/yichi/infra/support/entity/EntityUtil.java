package com.yichi.infra.support.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityUtil {
  private static final Logger logger = LoggerFactory.getLogger(EntityUtil.class);

  public EntityUtil() {}

  public static boolean isEntityInterface(Class<?> type) {
    return type.isAnnotationPresent(Entity.class);
  }

  public static String getImplClass(String baseName) {
    return String.format("%sImpl", baseName);
  }

  public static Class<?> getImplClass(Class<?> type) {
    if (isEntityInterface(type)) {
      try {
        return Class.forName(getImplClass(type.getCanonicalName()));
      } catch (ClassNotFoundException var2) {
        logger.error("Impl Class Not Found: " + type, var2);
        throw new RuntimeException(var2);
      }
    } else {
      return type;
    }
  }
}
