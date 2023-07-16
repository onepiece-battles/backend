package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.iam.model.RoleDao;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Dao
@Mapper
public interface UserDetailDao extends BaseDao<UserEntity, UUID> {

  @Results
  @Result(property = "userRoles", column = "id", one = @One(select = RoleDao.LIST_ROLES_BY_USER_ID))
  @Select("SELECT * FROM user_detail where username=#{username}")
  Optional<UserDetailImpl> getUserDetailByName(@Param("username") String username);
}
