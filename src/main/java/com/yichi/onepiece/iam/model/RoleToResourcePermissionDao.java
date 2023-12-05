package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import java.util.UUID;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Dao
@Mapper
public interface RoleToResourcePermissionDao extends BaseDao<RoleToResourcePermissionEntity, UUID> {

  @Delete("delete from role_to_resource_permission")
  void clearRoleToResourcePermission();

  @Select("select * from ro_to_resource_permission where role_id = #{roleId} and resource_permission_id = #{resourcePermissionId}")
  List<RoleToResourcePermissionEntity> checkPermission(
      @Param("roleId") UUID roleId,
      @Param("resourcePermissionId") UUID resourcePermissionId);

  @Select("select * from ro_to_resource_permission where role_id = #{roleId}")
  List<RoleToResourcePermissionEntity> listByRoleId(
      @Param("roleId") UUID roleId);
}
