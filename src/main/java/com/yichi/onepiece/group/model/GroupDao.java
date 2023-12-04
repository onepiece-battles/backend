package com.yichi.onepiece.group.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.mapstruct.Mapper;

import java.util.UUID;

@Dao
@Mapper
public interface GroupDao extends BaseDao<GroupEntity, UUID> {
}
