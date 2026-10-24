package com.yichi.onepiece.chatting.model;


import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.UUID;

@Dao
@Mapper
public interface UserConversationDao extends BaseDao<UserConversationEntity, UUID> {


    @Select("select * from `user_conversation` where `conversation_id` = #{id}")
    List<UserConversationEntity> listByConversationId(@Param("id") UUID conversationId);

    @Select("select * from `user_conversation` where `user_id` = #{id}")
    List<UserConversationEntity> listByUserId(@Param("id") UUID userId);

    @Select("select `last_read_message_id` from `user_conversation` where `user_id` = #{userId} and `conversation_id` = #{conversationId} ")
    UUID findLastReadMessageId(@Param("userId") UUID userId, @Param("conversationId") UUID conversationID);

    /// Single-row membership check used on the send-message hot path, instead
    /// of loading the user's entire conversation list and scanning it.
    @Select("select exists(select 1 from `user_conversation` "
            + "where `user_id` = #{userId} and `conversation_id` = #{conversationId})")
    boolean existsByUserIdAndConversationId(
            @Param("userId") UUID userId, @Param("conversationId") UUID conversationId);
}
