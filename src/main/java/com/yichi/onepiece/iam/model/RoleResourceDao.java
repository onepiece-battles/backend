package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.ResourceType;
import com.yichi.onepiece.model.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface RoleResourceDao extends BaseDao<RoleResourceEntity, UUID> {
  //  @Select({
  //    "<script>",
  //    " SELECT *",
  //    " FROM role_resource",
  //    " WHERE role_resource.role_id IN ",
  //    " <foreach item='item' collection='roleIds' open='(' separator=',' close=')'>",
  //    "   #{item}",
  //    " </foreach>",
  //    "</script>"
  //  })
  @Select({
    "<script>",
    "SELECT *",
    "FROM role_resource",
    "WHERE role_resource.role_id IN ",
    "<foreach item='id' collection='roleIds' open='(' separator=',' close=')'>",
    "  #{id}",
    "</foreach>",
    "</script>"
  })
  List<RoleResourceEntity> listRoleResourceByRoleIds(@Param("roleIds") Collection<UUID> roleIds);

  @Select("SELECT * FROM role_resource WHERE role_id = #{role_id} AND resource_id = #{resource_id}")
  Optional<RoleResourceEntity> findByRoleIdAndResourceId(
      @Param("role_id") UUID roleId, @Param("resource_id") UUID resourceId);

  @Select(
      "SELECT resource.name FROM role inner join role_resource ON (role.id = role_resource.role_id)"
          + " inner join resource on (role_resource.resource_id = resource.id) where role.name ="
          + " #{roleName}")
  List<ResourceType> getResourceNamesByRoleName(@Param("roleName") UserRole roleName);
}
