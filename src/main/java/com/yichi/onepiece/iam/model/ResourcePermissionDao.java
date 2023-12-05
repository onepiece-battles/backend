package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.ibatis.annotations.Mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface ResourcePermissionDao extends BaseDao<ResourcePermissionEntity, UUID> {


  @Select("select * from resource_permission")
  List<ResourcePermissionEntity> listAll();

  @Select("select * from resource_permission where resource_type = #{resourceType} and permission = #{permission}")
  Optional<ResourcePermissionEntity> findByResourceAndPermission(
      @Param("resourceType") ResourceType resourceType, @Param("permission") Permission permission);
}
