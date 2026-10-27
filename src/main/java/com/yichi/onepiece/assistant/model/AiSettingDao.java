package com.yichi.onepiece.assistant.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;
import java.util.UUID;

@Dao
@Mapper
public interface AiSettingDao extends BaseDao<AiSettingEntity, UUID> {
    @Select("select * from ai_setting order by creation_record_id desc limit 1")
    Optional<AiSettingEntity> findLatest();
}
