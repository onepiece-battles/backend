package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface UserDetailDao extends BaseDao<UserEntity, UUID> {

  @Select("SELECT * FROM user_detail where username=#{username}")
  public Optional<UserDetailImpl> getUserDetailByName(@Param("username") String username);
}
