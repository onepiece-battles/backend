package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.UserFilter;
import com.yichi.onepiece.model.UserSortableField;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Dao
@Mapper
public interface UserDao extends BaseDao<UserEntity, UUID> {

  @Select("SELECT * FROM user")
  List<UserEntity> getAllUsers();

  @ResultMap(value = "UserDetailView")
  List<UserDetailImpl> listUsers(
      @Param("userFilter") UserFilter userFilter,
      @Param("sortList") Collection<UserSortableField> sortList,
      @Param("offset") int offset,
      @Param("size") int size);

  @Select("select * from `user` where id = #{id}")
  Optional<UserDetailImpl> getById(@Param("id") UUID userId);

  int searchCount(@Param("userFilter") UserFilter userFilter);

  @Select("SELECT * FROM user WHERE name = #{name}")
  List<UserEntity> listByName(@Param("name") String name);

  @Select("SELECT * FROM user WHERE email= #{email}")
  List<UserEntity> listByEmailAddress(@Param("email") String email);
}
