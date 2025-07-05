package com.yichi.onepiece.notification.mapper;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;

import java.util.UUID;

@Dao
@Mapper
public interface NotificationDao extends BaseDao<NotificationEntity, UUID> {
}
