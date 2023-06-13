package com.yichi.onepiece.User.Model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface UserDao extends BaseDao<UserEntity, UUID> {

  @Select("SELECT * FROM user")
  List<UserEntity> getAllUsers();

  @Select("SELECT * FROM user WHERE user_name = #{user_name}")
  List<UserEntity> listByName(@Param("user_name") String userName);

  @Select("SELECT * FROM user WHERE email= #{email}")
  List<UserEntity> listByEmailAddress(@Param("email") String email);
}
