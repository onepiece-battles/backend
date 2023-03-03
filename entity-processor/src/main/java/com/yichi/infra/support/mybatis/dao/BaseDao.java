package com.yichi.infra.support.mybatis.dao;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

public interface BaseDao<T, K> {
  String KEY_NULL_FIELDS = "nullFields";
  String KEY_ENTITY = "entity";

  @InsertProvider(type = BaseSqlProvider.class, method = "insert")
  void insert(T var1);

  @InsertProvider(type = BaseSqlProvider.class, method = "insertIgnoreNull")
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insertIgnoreNull(T var1);

  @InsertProvider(type = BaseSqlProvider.class, method = "batchInsert")
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void batchInsert(List<T> var1);

  default int update(T t) {
    return this.update(t, (Set) null);
  }

  @UpdateProvider(type = BaseSqlProvider.class, method = "update")
  int update(@Param("entity") T var1, @Param("nullFields") Set<String> var2);

  @UpdateProvider(type = BaseSqlProvider.class, method = "fullUpdate")
  int fullUpdate(T var1);

  @SelectProvider(type = BaseSqlProvider.class, method = "findById")
  Optional<T> findById(K var1);

  @SelectProvider(type = BaseSqlProvider.class, method = "batchFindById")
  List<T> batchFindById(List<K> var1);
}
