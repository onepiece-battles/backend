package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.UserRole;
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
    "WHERE role.name IN ",
    "<foreach item='item' collection='names' open='(' separator=',' close=')'>",
    "  #{item}",
    "</foreach>",
    "</script>"
  })
  List<RoleEntity> listRoleByRoleNames(@Param("names") Collection<UserRole> names);

  @Select("SELECT * FROM role")
  List<RoleEntity> listAllRoles();

  @Select("SELECT name FROM role")
  Set<UserRole> listAllRoleNames();

  @Select("SELECT * FROM role WHERE role.name = #{name} LIMIT 1")
  Optional<RoleEntity> findByRoleName(@Param("name") UserRole name);

  @Select(
      "SELECT DISTINCT name FROM role where id in (SELECT role_id FROM user_role WHERE"
          + " user_role.user_id = #{userId})")
  Set<String> listRolesByUserId(@Param("userId") UUID userId);

  // todo(yichi): add user view to replace roleName by roleId in entity

  @Select(
      "select DISTINCT(resource_permission.resource_type) from role join"
          + " role_to_resource_permission on role.id = role_to_resource_permission.role_id join"
          + " resource_permission on role_to_resource_permission.resource_permission_id ="
          + " resource_permission.id where role.name = #{roleName}")
  List<ResourceType> listResourceTypeByRoleName(@Param("roleName") String roleName);
}
