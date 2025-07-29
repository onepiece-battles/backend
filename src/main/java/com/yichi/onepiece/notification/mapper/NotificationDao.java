package com.yichi.onepiece.notification.mapper;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

@Dao
@Mapper
public interface NotificationDao extends BaseDao<NotificationEntity, UUID> {
    @Select("select * from `notification` where `notification`.`notification_target_id` = #{userId}")
    List<NotificationEntity> list(@Param("userId") UUID userId);
    @Select("select * from `notification` where `notification`.`notification_target_id` = #{userId} and `notification`.`received` is false")
    List<NotificationEntity> listNew(@Param("userId") UUID userId);
}
