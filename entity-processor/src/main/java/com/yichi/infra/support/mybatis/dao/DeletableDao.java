package com.yichi.infra.support.mybatis.dao;

import java.util.List;
import org.apache.ibatis.annotations.DeleteProvider;

public interface DeletableDao<T, K> {
  @DeleteProvider(type = BaseSqlProvider.class, method = "deleteById")
  int deleteById(K var1);

  @DeleteProvider(type = BaseSqlProvider.class, method = "batchDeleteById")
  int batchDeleteById(List<K> var1);
}
