package com.yichi.onepiece.resource_sharing.model;

import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserResourceDao extends BaseDao<UserResourceEntity, UUID> {

  @Select("SELECT * FROM user_resource WHERE ")
  List<UserResourceEntity> listByUserIdAndResourceType(@Param("userId") UUID userId, @Param("resourceType") String resourceType);
}
