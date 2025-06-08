package com.yichi.onepiece.chatting.mapper;


import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

@Dao
@Mapper
public interface UserConversationDao extends BaseDao<UserConversationEntity, UUID> {


    @Select("select * from `user_conversation` where `conversation_id` = #{id}")
    List<UserConversationEntity> listByConversationId(@Param("id") UUID conversationId);

    @Select("select * from `user_conversation` where `user_id` = #{id}")
    List<UserConversationEntity> listByUserId(@Param("id") UUID userId);
}
