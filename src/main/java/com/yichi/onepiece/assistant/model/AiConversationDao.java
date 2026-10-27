package com.yichi.onepiece.assistant.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;
import java.util.UUID;

@Dao
@Mapper
public interface AiConversationDao extends BaseDao<AiConversationEntity, UUID> {
    @Select("select * from ai_conversation where user_id = #{userId} limit 1")
    Optional<AiConversationEntity> findByUserId(@Param("userId") UUID userId);
}
