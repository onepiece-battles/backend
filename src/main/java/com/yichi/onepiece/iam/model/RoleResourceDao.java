package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface RoleResourceDao extends BaseDao<RoleResourceEntity, UUID> {
  @Select({
    "<script>",
    "SELECT *",
    "FROM role_resource ",
    "WHERE role_resource.role_id in ",
    "<foreach item='item' collection='roleIds' open='(' separator=',' close=')'>",
    "  #{item}",
    "</foreach>",
    "</script>"
  })
  List<RoleResourceEntity> listRoleResourceByRoleIds(@Param("roleIds") Collection<UUID> roleIds);
}
