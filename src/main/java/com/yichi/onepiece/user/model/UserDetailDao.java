package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.iam.model.RoleDao;
import com.yichi.onepiece.iam.model.RoleResourceDao;
import com.yichi.onepiece.iam.model.TokenDao;
import com.yichi.onepiece.lineage.model.ActionRecordDao;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Dao
@Mapper
public interface UserDetailDao extends BaseDao<UserEntity, UUID> {

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
        @Result(property = "modificationRecordId", column = "modification_record_id")
      })
  @Select("SELECT * FROM user WHERE name=#{username}")
  Optional<UserDetailImpl> getUserDetailByName(@Param("username") String username);
}
