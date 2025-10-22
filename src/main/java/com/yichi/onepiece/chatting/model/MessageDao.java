package com.yichi.onepiece.chatting.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

@Dao
@Mapper
public interface MessageDao extends BaseDao<MessageEntity, UUID> {
    @Select("select * from message where conversation_id = #{conversationId} ORDER BY creation_record_id DESC LIMIT 1")
    MessageEntity getMostRecentMessage(@Param("conversationId") UUID conversationId);

    @Select("select * from message where conversation_id = #{conversationId} ORDER BY creation_record_id DESC LIMIT #{limit}")
    List<MessageEntity> listMessage(@Param("conversationId") UUID conversationId, @Param("limit") int limit);

    @Result(property = "preMessage", column = "pre_message_id",
            javaType = MessageEntity.class,
            one = @One(select = "findById"))
    @Select(" SELECT * FROM (select * from message where conversation_id = #{conversationId} ORDER BY creation_record_id DESC LIMIT #{limit}) as t ORDER BY creation_record_id ASC")
    List<MessageFullView> listMessageFullView(@Param("conversationId") UUID conversationId, @Param("limit") int limit);

    @Result(property = "preMessage", column = "pre_message_id",
            javaType = MessageEntity.class,
            one = @One(select = "findById"))
    @Select("select * from `message` where `message`.`conversation_id` = #{conversationId} and `message`.`creation_record_id` >= #{messageDigit}")
    List<MessageFullView> listMessageFullViewByLastMessageId(@Param("conversationId") UUID conversationId, @Param("messageDigit") Long messageDigit);
}
