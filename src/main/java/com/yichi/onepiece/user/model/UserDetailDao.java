package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.iam.model.RoleDao;
import com.yichi.onepiece.iam.model.TokenDao;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Dao
@Mapper
public interface UserDetailDao extends BaseDao<UserEntity, UUID> {
  @Result(
      property = "userRoles",
      column = "id",
      many = @Many(select = RoleDao.LIST_ROLES_BY_USER_ID))
  @Result(
      property = "tokens",
      column = "id",
      many = @Many(select = TokenDao.LIST_TOKENS_BY_USER_ID))
  @Result(property = "id", column = "id")
  @Select("SELECT * FROM user WHERE name=#{username}")
  Optional<UserDetailImpl> getUserDetailByName(@Param("username") String username);
}
