package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.iam.model.RoleDao;
import com.yichi.onepiece.iam.model.RoleResourceDao;
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

  //  @Results(
  //      id = "UserDetailView",
  //      value = {
  //        @Result(
  //            property = "creationRecord",
  //            column = "creation_record_id",
  //            one = @One(select = ActionRecordDao.FIND_BY_ID)),
  //        @Result(
  //            property = "modificationRecord",
  //            column = "modification_record_id",
  //            one = @One(select = ActionRecordDao.FIND_BY_ID)),
  //        @Result(
  //            property = "userRoles",
  //            column = "id",
  //            many = @Many(select = RoleDao.LIST_ROLES_BY_USER_ID)),
  //        @Result(
  //            property = "tokens",
  //            column = "id",
  //            many = @Many(select = TokenDao.LIST_TOKENS_BY_USER_ID)),
  //        @Result(
  //            property = "loginRoleResources",
  //            column = "login_role",
  //            many = @Many(select = RoleResourceDao.getResourceByRole)),
  //        @Result(property = "id", column = "id"),
  //        @Result(property = "creationRecordId", column = "creation_record_id"),
  //        @Result(property = "modificationRecordId", column = "modification_record_id"),
  //        @Result(property = "loginRole", column = "login_role")
  //      })
  List<UserDetailImpl> listUsers(
      @Param("userFilter") UserFilter userFilter,
      @Param("sortList") Collection<UserSortableField> sortList,
      @Param("offset") int offset,
      @Param("size") int size);

  @Select("select * from `user` where id = #{id}")
  //  @ResultMap(value = "UserDetailView")
  Optional<UserDetailImpl> getById(@Param("id") UUID userId);

  int searchCount(@Param("userFilter") UserFilter userFilter);

  @Select("SELECT * FROM user WHERE name = #{name}")
  List<UserEntity> listByName(@Param("name") String name);

  @Select("SELECT * FROM user WHERE email= #{email}")
  List<UserEntity> listByEmailAddress(@Param("email") String email);

  @Results(
      id = "UserDetailView",
      value = {
        @Result(
            property = "creationRecord",
            column = "creation_record_id",
            one = @One(select = ActionRecordDao.FIND_BY_ID)),
        @Result(
            property = "modificationRecord",
            column = "modification_record_id",
            one = @One(select = ActionRecordDao.FIND_BY_ID)),
        @Result(
            property = "userRoles",
            column = "id",
            many = @Many(select = RoleDao.LIST_ROLES_BY_USER_ID)),
        @Result(
            property = "tokens",
            column = "id",
            many = @Many(select = TokenDao.LIST_TOKENS_BY_USER_ID)),
        @Result(
            property = "loginRoleResources",
            column = "login_role",
            many = @Many(select = RoleResourceDao.getResourceByRole)),
        @Result(property = "id", column = "id"),
        @Result(property = "creationRecordId", column = "creation_record_id"),
        @Result(property = "modificationRecordId", column = "modification_record_id"),
        @Result(property = "loginRole", column = "login_role")
      })
  @Select("SELECT * FROM user WHERE name=#{username}")
  Optional<UserDetailImpl> getUserDetailByName(@Param("username") String username);
}
