package com.yichi.onepiece.assistant.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

@Dao
@Mapper
public interface AiMessageDao extends BaseDao<AiMessageEntity, UUID> {
    @Select("select * from ai_message where conversation_id = #{conversationId} "
            + "order by creation_record_id asc limit #{limit}")
    List<AiMessageEntity> listByConversation(
            @Param("conversationId") UUID conversationId, @Param("limit") int limit);

    @Delete("delete from ai_message where conversation_id = #{conversationId}")
    void deleteByConversation(@Param("conversationId") UUID conversationId);
}
