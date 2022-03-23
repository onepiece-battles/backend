package com.yichi.infra.support.mybatis.dao;

import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Options;

public interface AutoGenIdDao<T> extends BaseDao<T, Long> {
  @InsertProvider(type = BaseSqlProvider.class, method = "insert")
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(T var1);
}
