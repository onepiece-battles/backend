package com.yichi.infra.support.mybatis.dao;

import com.google.common.base.Preconditions;
import com.google.common.collect.UnmodifiableIterator;
import com.yichi.infra.support.mybatis.dao.EntityInfo.EntityField;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.builder.annotation.ProviderContext;
import org.apache.commons.lang3.StringUtils;

public class BaseSqlProvider {
  private static final Map<String, EntityInfo> cachedEntityInfo = new HashMap<>();

  public BaseSqlProvider() {}

  static EntityInfo getEntityInfo(Class<?> clz) {
    String key = clz.getName();
    synchronized (cachedEntityInfo) {
      if (!cachedEntityInfo.containsKey(key)) {
        cachedEntityInfo.put(key, EntityInfo.create(clz));
      }

      return (EntityInfo) cachedEntityInfo.get(key);
    }
  }

  private static Class<?> getDataType(ProviderContext context) {
    return DaoUtils.getEntityType(context.getMapperType());
  }

  private static Class<?> getKeyType(ProviderContext context) {
    return DaoUtils.getKeyType(context.getMapperType());
  }

  private String toParameter(String entityName, EntityField field) {
    return this.toParameter(entityName, field, false);
  }

  private String toParameter(String entityName, EntityField field, boolean addJavaType) {
    if (field.typeHandler == null) {
      if (addJavaType) {
        return StringUtils.isEmpty(entityName)
            ? String.format("#{%s, javaType=%s}", field.fieldName, field.javaType.getName())
            : String.format(
                "#{%s.%s, javaType=%s}", entityName, field.fieldName, field.javaType.getName());
      } else {
        return StringUtils.isEmpty(entityName)
            ? String.format("#{%s}", field.fieldName)
            : String.format("#{%s.%s}", entityName, field.fieldName);
      }
    } else {
      return StringUtils.isEmpty(entityName)
          ? String.format("#{%s, typeHandler=%s}", field.fieldName, field.typeHandler.getName())
          : String.format(
              "#{%s.%s, typeHandler=%s}", entityName, field.fieldName, field.typeHandler.getName());
    }
  }

  public String insert(Object data, ProviderContext context) {
    Class<?> dataType = getDataType(context);
    Preconditions.checkArgument(
        dataType.isInstance(data), "The insert data must be an instance of " + dataType);
    EntityInfo entityInfo = getEntityInfo(dataType);
    StringBuilder cols = new StringBuilder();
    StringBuilder fields = new StringBuilder();

    EntityField field;
    for (UnmodifiableIterator var7 = entityInfo.getFields().iterator();
        var7.hasNext();
        fields.append(this.toParameter((String) null, field))) {
      field = (EntityField) var7.next();
      if (cols.length() > 0) {
        cols.append(",");
      }

      cols.append('`').append(field.columnName).append('`');
      if (fields.length() > 0) {
        fields.append(",");
      }
    }

    entityInfo.tryAutoFillId(data);
    return String.format(
        "insert into `%s` (%s) values (%s)", entityInfo.getTableName(), cols, fields);
  }

  public String insertIgnoreNull(Object data, ProviderContext context) {
    Class<?> dataType = getDataType(context);
    Preconditions.checkArgument(
        dataType.isInstance(data), "The insert data must be an instance of " + dataType);
    EntityInfo entityInfo = getEntityInfo(dataType);
    StringBuilder cols = new StringBuilder();
    StringBuilder fields = new StringBuilder();
    UnmodifiableIterator var7 = entityInfo.getFields().iterator();

    while (var7.hasNext()) {
      EntityField field = (EntityField) var7.next();
      cols.append(
          String.format("<if test='%s != null'>`%s`,</if>", field.fieldName, field.columnName));
      fields.append(
          String.format(
              "<if test='%s != null'>%s,</if>",
              field.fieldName, this.toParameter((String) null, field)));
    }

    entityInfo.tryAutoFillId(data);
    return String.format(
        "<script>insert into `%s` <trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">%s</trim>"
            + " values <trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">%s</trim></script>",
        entityInfo.getTableName(), cols, fields);
  }

  public String batchInsert(List<?> data, ProviderContext context) {
    Class<?> dataType = getDataType(context);
    Preconditions.checkArgument(!data.isEmpty(), "the insert list can not be empty");
    Iterator var4 = data.iterator();

    while (var4.hasNext()) {
      Object item = var4.next();
      Preconditions.checkArgument(
          dataType.isInstance(item), "The insert data must be an instance of " + dataType);
    }

    EntityInfo entityInfo = getEntityInfo(dataType);
    StringBuilder cols = new StringBuilder();
    StringBuilder fields = new StringBuilder();

    EntityField field;
    for (UnmodifiableIterator var7 = entityInfo.getFields().iterator();
        var7.hasNext();
        fields.append(this.toParameter("item", field))) {
      field = (EntityField) var7.next();
      if (cols.length() > 0) {
        cols.append(",");
      }

      cols.append('`').append(field.columnName).append('`');
      if (fields.length() > 0) {
        fields.append(",");
      }
    }

    Iterator var11 = data.iterator();

    while (var11.hasNext()) {
      Object item = var11.next();
      entityInfo.tryAutoFillId(item);
    }

    return String.format(
        "<script>insert into `%s` (%s) values <foreach collection='list' separator=','"
            + " item='item'> (%s)</foreach></script>",
        entityInfo.getTableName(), cols, fields);
  }

  public String update(ParamMap<Object> paramMap, ProviderContext context) {
    Class<?> dataType = getDataType(context);
    Object data = paramMap.get("entity");
    Preconditions.checkArgument(
        dataType.isInstance(data), "The update data must be an instance of " + dataType);
    EntityInfo entityInfo = getEntityInfo(dataType);
    StringBuilder setSql = new StringBuilder();
    EntityField keyId = (EntityField) Preconditions.checkNotNull(entityInfo.getIdColumn());
    Set<String> nullFields = new HashSet();
    if (paramMap.containsKey("nullFields")) {
      Set<String> nullFieldsParams = (Set) paramMap.get("nullFields");
      if (nullFieldsParams != null) {
        nullFields.addAll(nullFieldsParams);
      }
    }

    nullFields.forEach(
        (fieldName) -> {
          Preconditions.checkArgument(
              !fieldName.equals(keyId.fieldName), "Update id column to null is not allowed");
          EntityField field =
              (EntityField)
                  entityInfo
                      .findField(fieldName)
                      .orElseThrow(
                          () -> {
                            return new IllegalArgumentException(
                                "Field (" + fieldName + ") not exist");
                          });
          Preconditions.checkArgument(
              !field.ignoreUpdates, "Field (" + fieldName + ") is not allow to update");
          setSql.append(String.format("`%s`=null,", field.columnName));
        });
    UnmodifiableIterator var11 = entityInfo.getFields().iterator();

    while (var11.hasNext()) {
      EntityField field = (EntityField) var11.next();
      if (!field.ignoreUpdates
          && !field.columnName.equals(keyId.columnName)
          && !nullFields.contains(field.fieldName)) {
        setSql.append(
            String.format(
                "<if test='%s.%s != null'>`%s`=%s,</if>",
                "entity",
                field.fieldName,
                field.columnName,
                this.toParameter("entity", field, true)));
      }
    }

    return String.format(
        "<script>update `%s` <set>%s</set> where `%s`=%s</script>",
        entityInfo.getTableName(), setSql, keyId.columnName, this.toParameter("entity", keyId));
  }

  public String fullUpdate(Object data, ProviderContext context) {
    Class<?> dataType = getDataType(context);
    Preconditions.checkArgument(
        dataType.isInstance(data), "The update data must be an instance of " + dataType);
    EntityInfo entityInfo = getEntityInfo(dataType);
    StringBuilder setSql = new StringBuilder();
    EntityField keyId = (EntityField) Preconditions.checkNotNull(entityInfo.getIdColumn());
    UnmodifiableIterator var7 = entityInfo.getFields().iterator();

    while (var7.hasNext()) {
      EntityField field = (EntityField) var7.next();
      String col = field.columnName;
      if (!field.ignoreUpdates && !col.equals(keyId.columnName)) {
        setSql.append(String.format("`%s`=%s,", col, this.toParameter((String) null, field)));
      }
    }

    return String.format(
        "<script>update `%s` <set>%s</set> where `%s`=%s</script>",
        entityInfo.getTableName(),
        setSql,
        keyId.columnName,
        this.toParameter((String) null, keyId));
  }

  public String findById(Object id, ProviderContext context) {
    EntityInfo entityInfo = getEntityInfo(getDataType(context));
    EntityField keyId =
        (EntityField) Preconditions.checkNotNull(entityInfo.getIdColumn(), "no pk defined");
    return String.format(
        "select * from `%s` where `%s`=%s",
        entityInfo.getTableName(),
        keyId.columnName,
        this.toParameter((String) null, entityInfo.getIdColumn()));
  }

  public String batchFindById(List<?> ids, ProviderContext context) {
    Preconditions.checkArgument(!ids.isEmpty(), "the id list can not be empty");
    EntityInfo entityInfo = getEntityInfo(getDataType(context));
    EntityField keyId =
        (EntityField) Preconditions.checkNotNull(entityInfo.getIdColumn(), "no pk defined");
    return String.format(
        "<script>select * from `%s` where `%s` in <foreach collection='list' item='id' open='('"
            + " close=')' separator=','>%s</foreach></script>",
        entityInfo.getTableName(),
        keyId.columnName,
        this.toParameter((String) null, entityInfo.getIdColumn()));
  }

  public String deleteById(Object id, ProviderContext context) {
    Class<?> keyType = getKeyType(context);
    Preconditions.checkArgument(
        keyType.isInstance(id), "The key must be an instance of " + keyType);
    EntityInfo entityInfo = getEntityInfo(getDataType(context));
    EntityField keyId =
        (EntityField) Preconditions.checkNotNull(entityInfo.getIdColumn(), "no pk defined");
    return String.format(
        "delete from `%s` where `%s`=%s",
        entityInfo.getTableName(),
        keyId.columnName,
        this.toParameter((String) null, entityInfo.getIdColumn()));
  }

  public String batchDeleteById(List<?> ids, ProviderContext context) {
    Class<?> keyType = getKeyType(context);
    Preconditions.checkArgument(!ids.isEmpty(), "the id list can not be empty");
    Iterator var4 = ids.iterator();

    while (var4.hasNext()) {
      Object id = var4.next();
      Preconditions.checkArgument(
          keyType.isInstance(id), "The id must be an instance of " + keyType);
    }

    EntityInfo entityInfo = getEntityInfo(getDataType(context));
    EntityField keyId =
        (EntityField) Preconditions.checkNotNull(entityInfo.getIdColumn(), "no pk defined");
    return String.format(
        "<script>delete from `%s` where `%s` in <foreach collection='list' item='id' open='('"
            + " close=')' separator=','>%s</foreach></script>",
        entityInfo.getTableName(),
        keyId.columnName,
        this.toParameter((String) null, entityInfo.getIdColumn()));
  }
}
