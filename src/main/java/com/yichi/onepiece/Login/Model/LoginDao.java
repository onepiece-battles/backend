package com.yichi.onepiece.Login.Model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
public interface LoginDao extends BaseDao<UUID, UserEntity> {

  @Select("SELECT * FROM user id = #{id}")
  UserEntity findById(@Param("id") UUID id);

  @Select("SELECT * FROM user")
  List<UserEntity> listUsers();
}
