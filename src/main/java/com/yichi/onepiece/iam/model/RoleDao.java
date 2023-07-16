package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface RoleDao extends BaseDao<RoleEntity, UUID> {
  String LIST_ROLES_BY_USER_ID = "com.yichi.onepiece.iam.model.RoleDao.listRolesByUserId";

  @Select({
    "<script>",
    "SELECT *",
    "FROM role ",
    "WHERE role.name in ",
    "<foreach item='item' collection='roleNames' open='(' separator=',' close=')'>",
    "  #{item}",
    "</foreach>",
    "</script>"
  })
  List<RoleEntity> listRoleByRoleNames(@Param("roleNames") Collection<String> roleNames);

  @Select("SELECT * FROM role")
  List<RoleEntity> listAllRoles();

  @Select("SELECT * FROM role WHERE name = #{roleName}")
  Optional<RoleEntity> findByRoleName(@Param("roleName") String roleName);

  @Select(
      "SELECT DISTINCT name FROM role where id in (SELECT role_id FROM user_role WHERE"
          + " user_role.user_id = #{userId})")
  Set<String> listRolesByUserId(@Param("userId") UUID userId);
}
