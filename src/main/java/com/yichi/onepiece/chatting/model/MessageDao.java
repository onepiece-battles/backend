package com.yichi.onepiece.chatting.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Dao
@Mapper
public interface MessageDao extends BaseDao<MessageEntity, UUID> {
    // SQL + result mapping defined in messageDao.xml (JOIN-based, no N+1).
    MessageFullView getMostRecentMessage(@Param("conversationId") UUID conversationId);

    @Select("select * from message where conversation_id = #{conversationId} ORDER BY creation_record_id DESC LIMIT #{limit}")
    List<MessageEntity> listMessage(@Param("conversationId") UUID conversationId, @Param("limit") int limit);

    // SQL + result mapping defined in messageDao.xml (JOIN-based, no N+1).
    List<MessageFullView> listMessageFullView(@Param("conversationId") UUID conversationId, @Param("limit") int limit);

    // SQL + result mapping defined in messageDao.xml (JOIN-based, no N+1).
    List<MessageFullView> listMessageFullViewByLastMessageId(
            @Param("conversationId") UUID conversationId,
            @Param("messageDigit") Long messageDigit,
            @Param("limit") int limit);

    // SQL + result mapping defined in messageDao.xml (JOIN-based, no N+1).
    Optional<MessageFullView> getMessageFullView(@Param("id") UUID id);
}
