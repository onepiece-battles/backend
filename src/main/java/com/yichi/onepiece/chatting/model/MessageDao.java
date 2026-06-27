package com.yichi.onepiece.chatting.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Dao
@Mapper
public interface MessageDao extends BaseDao<MessageEntity, UUID> {
    @Result(property = "preMessage", column = "pre_message_id",
            javaType = MessageEntity.class,
            one = @One(select = "findById"))
    @Result(property = "creationRecord", column = "creation_record_id", javaType = ActionRecordEntity.class,
            one = @One(select = "com.yichi.onepiece.lineage.model.ActionRecordDao.findById")
    )
    @Result(property = "modificationRecord", column = "modification_record_id", javaType = ActionRecordEntity.class,
            one = @One(select = "com.yichi.onepiece.lineage.model.ActionRecordDao.findById")
    )
    @Select("select * from message where conversation_id = #{conversationId} ORDER BY creation_record_id DESC LIMIT 1")
    MessageFullView getMostRecentMessage(@Param("conversationId") UUID conversationId);

    @Select("select * from message where conversation_id = #{conversationId} ORDER BY creation_record_id DESC LIMIT #{limit}")
    List<MessageEntity> listMessage(@Param("conversationId") UUID conversationId, @Param("limit") int limit);

    @Result(property = "preMessage", column = "pre_message_id",
            javaType = MessageEntity.class,
            one = @One(select = "findById"))
    @Result(property = "creationRecord", column = "creation_record_id", javaType = ActionRecordEntity.class,
            one = @One(select = "com.yichi.onepiece.lineage.model.ActionRecordDao.findById")
    )
    @Result(property = "modificationRecord", column = "modification_record_id", javaType = ActionRecordEntity.class,
            one = @One(select = "com.yichi.onepiece.lineage.model.ActionRecordDao.findById")
    )
    @Select(" SELECT * FROM (select * from message where conversation_id = #{conversationId} ORDER BY creation_record_id DESC LIMIT #{limit}) as t ORDER BY creation_record_id ASC")
    List<MessageFullView> listMessageFullView(@Param("conversationId") UUID conversationId, @Param("limit") int limit);

    @Result(property = "preMessage", column = "pre_message_id",
            javaType = MessageEntity.class,
            one = @One(select = "findById"))
    @Result(property = "creationRecord", column = "creation_record_id", javaType = ActionRecordEntity.class,
            one = @One(select = "com.yichi.onepiece.lineage.model.ActionRecordDao.findById")
    )
    @Result(property = "modificationRecord", column = "modification_record_id", javaType = ActionRecordEntity.class,
            one = @One(select = "com.yichi.onepiece.lineage.model.ActionRecordDao.findById")
    )
    @Select("select * from `message` where `message`.`conversation_id` = #{conversationId} and `message`.`creation_record_id` >= #{messageDigit} order by `message`.`creation_record_id` asc limit #{limit}")
    List<MessageFullView> listMessageFullViewByLastMessageId(
            @Param("conversationId") UUID conversationId,
            @Param("messageDigit") Long messageDigit,
            @Param("limit") int limit);


    @Result(property = "preMessage", column = "pre_message_id",
            javaType = MessageEntity.class,
            one = @One(select = "findById"))
    @Result(property = "creationRecord", column = "creation_record_id", javaType = ActionRecordEntity.class,
            one = @One(select = "com.yichi.onepiece.lineage.model.ActionRecordDao.findById")
    )
    @Result(property = "modificationRecord", column = "modification_record_id", javaType = ActionRecordEntity.class,
            one = @One(select = "com.yichi.onepiece.lineage.model.ActionRecordDao.findById")
    )
    @Select("select * from `message` where `message`.`id` = #{id}")
    Optional<MessageFullView> getMessageFullView(@Param("id") UUID id);
}
