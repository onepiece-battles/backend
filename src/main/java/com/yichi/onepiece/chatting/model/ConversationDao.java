package com.yichi.onepiece.chatting.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

@Dao
@Mapper
public interface ConversationDao extends BaseDao<ConversationEntity, UUID> {

    ConversationEntity createConversation(ConversationEntity conversationEntity);

    @ResultMap("conversationFullView")
    @Select("select * from `conversation` left join `user_conversation` on `conversation`.`id` = `user_conversation`.`conversation_id` where `user_conversation`.`user_id` = #{userId} ")
    List<ConversationFullView> listChat(@Param("userId") UUID userId);

    // list all conversation that contains exactly the users in input param.
    @Select({"<script>",
            "SELECT c.*",
            "FROM conversation c",
            "JOIN user_conversation uc ON c.id = uc.conversation_id",
            "WHERE uc.user_id IN",
            "<foreach item='userId' collection='userIds' open='(' separator=',' close=')'>#{userId}</foreach>",
            "GROUP BY c.id",
            "HAVING COUNT(DISTINCT uc.user_id) = #{size}",
            "AND COUNT(DISTINCT uc.user_id) = (",
            "   SELECT COUNT(DISTINCT uc2.user_id)",
            "   FROM user_conversation uc2",
            "   WHERE uc2.conversation_id = c.id",
            ")",
            "</script>"
    })
    List<ConversationEntity> listByUserIds(@Param("userIds") List<UUID> userIds, @Param("size") Integer size);
}
