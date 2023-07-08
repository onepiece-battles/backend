package com.yichi.onepiece.Iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;

@Dao
@Mapper
public interface ResourceDao extends BaseDao<ResourceEntity, UUID> {}
