package com.yichi.onepiece.Login.Model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface LoginDao extends BaseDao<UserEntity, UUID> {

  @Select("SELECT * FROM user WHERE id = #{id}")
  Optional<UserEntity> getById(@Param("id") UUID id);

  @Select("SELECT * FROM user")
  List<UserEntity> getAllUsers();
}
