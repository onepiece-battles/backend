package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.UUID;

@Dao
@Mapper
public interface ResourcePermissionDao extends BaseDao<ResourcePermissionEntity, UUID> {
}
