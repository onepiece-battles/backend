package com.yichi.onepiece.group.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;

@Dao
@Mapper
public interface GroupDao extends BaseDao<GroupEntity, UUID> {
}
