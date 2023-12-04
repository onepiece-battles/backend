package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.UUID;


@Dao
@Mapper
public interface RoleToResourcePermissionDao extends BaseDao<RoleToResourcePermissionEntity, UUID> {
}
