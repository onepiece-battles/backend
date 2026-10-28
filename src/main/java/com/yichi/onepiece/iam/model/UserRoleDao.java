package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface UserRoleDao extends BaseDao<UserRoleEntity, UUID> {
  @Select("select user_role.role_id from user_role where user_role.user_id = #{userId}")
  List<UUID> getByUserId(@Param("userId") UUID userId);

  /// Ids of users that have no row in user_role at all. Such an account resolves
  /// to zero authorities, so every permission-checked endpoint rejects it with a
  /// 401 (AuthorizationServiceException) before any permission is even evaluated.
  @Select("select `user`.`id` from `user` "
          + "where not exists (select 1 from `user_role` where `user_role`.`user_id` = `user`.`id`)")
  List<UUID> listUserIdsWithoutRole();
}
