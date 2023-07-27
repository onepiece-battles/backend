package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface ResourceDao extends BaseDao<ResourceEntity, UUID> {
  @Select("SELECT DISTINCT name FROM resource")
  Set<String> listAllResourceNames();

  @Select("SELECT * FROM resource WHERE name = #{name}")
  Optional<ResourceEntity> getResourceByName(@Param("name") String name);
}
