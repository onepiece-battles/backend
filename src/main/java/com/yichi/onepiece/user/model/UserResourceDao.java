package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.ResourceType;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface UserResourceDao extends BaseDao<UserResourceEntity, UUID> {

  @Select(
      "SELECT * FROM user_resource WHERE user_id = #{userId} AND resource_type = #{resourceType}")
  Optional<UserResourceEntity> getByUserIdAndResourceType(
      @Param("userId") UUID userId, @Param("resourceType") ResourceType resourceType);
}
