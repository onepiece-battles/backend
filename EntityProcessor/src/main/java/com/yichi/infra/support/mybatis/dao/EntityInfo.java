package com.yichi.infra.support.mybatis.dao;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.yichi.infra.support.entity.EntityImpl;
import com.yichi.infra.support.mybatis.annotations.Column;
import com.yichi.infra.support.mybatis.annotations.Table;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.ObjectTypeHandler;

public class EntityInfo {
  private static final String ENTITY_CLASS_SUFFIX = "Entity";
  private static final String ID = "id";
  private final String tableName;
  private final EntityInfo.EntityField idColumn;
  private final ImmutableList<EntityField> fields;
  private final Class<?> javaType;

  static EntityInfo create(Class<?> clz) {
    Table tableAnnotation = (Table) clz.getAnnotation(Table.class);
    String tableName;
    if (tableAnnotation != null) {
      tableName = tableAnnotation.name();
    } else {
      String name = clz.getSimpleName();
      if (name.endsWith("Entity")) {
        name = name.substring(0, name.length() - "Entity".length());
      }

      tableName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);
    }

    Builder<EntityField> fields = ImmutableList.builder();
    Method[] methods = clz.getMethods();
    EntityInfo.EntityField idColumn = null;
    Set<String> fieldNames = new HashSet();
    Method[] var7 = methods;
    int var8 = methods.length;

    int var9;
    String fieldName;
    String columnName;
    EntityInfo.EntityField entityField;
    for (var9 = 0; var9 < var8; ++var9) {
      Method method = var7[var9];
      if (!Modifier.isStatic(method.getModifiers())
          && !method.getDeclaringClass().equals(Object.class)) {
        String methodName = method.getName();
        if (methodName.startsWith("get") || methodName.length() <= 3) {
          String var10000 = methodName.substring(3, 4).toLowerCase();
          fieldName = var10000 + methodName.substring(4);
          columnName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
          entityField = new EntityInfo.EntityField(fieldName, columnName, method);
          fields.add(entityField);
          fieldNames.add(fieldName);
          if (columnName.equals("id")) {
            idColumn = entityField;
          }
        }
      }
    }

    Field[] attributes = clz.getDeclaredFields();
    Field[] var17 = attributes;
    var9 = attributes.length;

    for (int var18 = 0; var18 < var9; ++var18) {
      Field field = var17[var18];
      if (!Modifier.isStatic(field.getModifiers())) {
        fieldName = field.getName();
        if (!fieldNames.contains(fieldName)) {
          columnName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
          entityField = new EntityInfo.EntityField(fieldName, columnName, field);
          fields.add(entityField);
          if (columnName.equals("id")) {
            idColumn = entityField;
          }
        }
      }
    }

    return new EntityInfo(clz, tableName, idColumn, fields.build());
  }

  String getTableName() {
    return this.tableName;
  }

  Class<?> getJavaType() {
    return this.javaType;
  }

  EntityInfo.EntityField getIdColumn() {
    return this.idColumn;
  }

  Optional<EntityField> findField(String name) {
    return this.fields.stream()
        .filter(
            (field) -> {
              return field.fieldName.equals(name);
            })
        .findFirst();
  }

  ImmutableList<EntityInfo.EntityField> getFields() {
    return this.fields;
  }

  void tryAutoFillId(Object obj) {
    if (obj instanceof EntityImpl
        && this.idColumn != null
        && this.idColumn.javaType == UUID.class) {
      EntityImpl impl = (EntityImpl) obj;

      try {
        if (impl.get("id") == null) {
          impl.set("id", UUID.randomUUID());
        }
      } catch (NoSuchFieldException var4) {
        throw new RuntimeException(var4);
      }
    }
  }

  private EntityInfo(
      Class<?> javaType,
      String tableName,
      EntityInfo.EntityField idColumn,
      ImmutableList<EntityInfo.EntityField> fields) {
    this.javaType = javaType;
    this.tableName = tableName;
    this.idColumn = idColumn;
    this.fields = fields;
  }

  static class EntityField {
    final String fieldName;
    final String columnName;
    final Class<? extends BaseTypeHandler<?>> typeHandler;
    final Class<?> javaType;
    final Annotation[] annotations;
    final boolean ignoreUpdates;
    final Type genericJavaType;
    final Column columnAnnotation;

    EntityField(String fieldName, String columnName, Method method) {
      this.fieldName = fieldName;
      this.columnName = columnName;
      this.javaType = method.getReturnType();
      this.annotations = method.getAnnotations();
      this.genericJavaType = method.getGenericReturnType();
      this.columnAnnotation = (Column) method.getAnnotation(Column.class);
      if (this.columnAnnotation != null) {
        this.ignoreUpdates = this.columnAnnotation.ignoreUpdates();
        this.typeHandler =
            this.columnAnnotation.typeHandler() == ObjectTypeHandler.class
                ? null
                : this.columnAnnotation.typeHandler();
      } else {
        this.ignoreUpdates = false;
        this.typeHandler = null;
      }
    }

    EntityField(String fieldName, String columnName, Field field) {
      this.fieldName = fieldName;
      this.columnName = columnName;
      this.javaType = field.getType();
      this.annotations = field.getAnnotations();
      this.genericJavaType = field.getGenericType();
      this.columnAnnotation = (Column) field.getAnnotation(Column.class);
      if (this.columnAnnotation != null) {
        this.ignoreUpdates = this.columnAnnotation.ignoreUpdates();
        this.typeHandler =
            this.columnAnnotation.typeHandler() == ObjectTypeHandler.class
                ? null
                : this.columnAnnotation.typeHandler();
      } else {
        this.ignoreUpdates = false;
        this.typeHandler = null;
      }
    }
  }
}
