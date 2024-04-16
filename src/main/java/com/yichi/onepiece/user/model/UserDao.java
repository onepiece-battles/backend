package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.iam.model.RoleDao;
import com.yichi.onepiece.iam.model.TokenDao;
import com.yichi.onepiece.lineage.model.ActionRecordDao;
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
  List<UserDetailImpl> listUsers(
      @Param("userFilter") UserFilter userFilter,
      @Param("sortList") Collection<UserSortableField> sortList,
      @Param("offset") int offset,
      @Param("size") int size);


  @ResultMap("userDetailView")
  @Select("select * from `user` where id = #{id}")
  Optional<UserDetailImpl> getById(@Param("id") UUID userId);

  int searchCount(@Param("userFilter") UserFilter userFilter);

  @Select("SELECT * FROM user WHERE nickname = #{nickname}")
  List<UserEntity> listByNickName(@Param("nickname") String nickname);

  @Select("SELECT * FROM user WHERE username= #{email}")
  List<UserEntity> listByEmailAddress(@Param("email") String email);

  @Select("select * from `user` where `user`.`username` = #{username}")
  Optional<UserEntity> getByUsername(@Param("username") String username);

  @ResultMap("userDetailView")
  @Select("SELECT * FROM user WHERE username=#{userName}")
  Optional<UserDetailImpl> getUserByUsername(@Param("userName") String userName);
}
