package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Dao
@Mapper
public interface ResourceDao extends BaseDao<ResourceEntity, UUID> {
  @Delete("DELETE FROM resource")
  void deleteAllRecords();
}
