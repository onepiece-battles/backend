package com.yichi.onepiece.user.model;

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

  @Select("SELECT * FROM user WHERE name = #{name}")
  List<UserEntity> listByName(@Param("name") String name);

  @Select("SELECT * FROM user WHERE email= #{email}")
  List<UserEntity> listByEmailAddress(@Param("email") String email);
}
