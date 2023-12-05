package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.UUID;
import org.apache.ibatis.annotations.Select;


@Dao
@Mapper
public interface RoleToResourcePermissionDao extends BaseDao<RoleToResourcePermissionEntity, UUID> {

  @Delete("delete from role_to_resource_permission")
  void clearRoleToResourcePermission();
}
