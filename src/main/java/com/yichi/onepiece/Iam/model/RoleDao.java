package com.yichi.onepiece.Iam.model;

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
public interface RoleDao extends BaseDao<RoleEntity, UUID> {
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
}
