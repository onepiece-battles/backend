package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.UUID;

@Dao
public interface FriendshipDao extends BaseDao<FriendshipEntity, UUID> {
    @Select("select * from `friendship` f where (f.`friend_left_id` = #{friendA} and f.`friend_right_id` = #{friendB}) or  (f.`friend_left_id` = #{friendB} and f.`friend_right_id` = #{friendA})")
    List<FriendshipEntity> findFriendShipById(@Param("friendA") UUID friendLeft, @Param("friendB") UUID friendRight);

    @Select({
            "<script>",
            "SELECT * ",
            "FROM `friendship` ",
            "where #{userId} in (`friendship`.`friend_left_id`, `friendship`.`friend_right_id`)  and `friendship`.`friendship_status` = #{status} ",
            "</script>"
    })
    List<FriendshipEntity> listFriendshipsByUserId(@Param("userId") UUID userId, @Param("status") FriendshipStatus status);

    @Update("UPDATE `friendship` SET `friendship_status` = 'ACCEPTED' "
            + "WHERE `friend_left_id` = #{applicantId} AND `friend_right_id` = #{recipientId} "
            + "AND `friendship_status` = 'APPLIED'")
    int acceptPendingRequests(@Param("applicantId") UUID applicantId, @Param("recipientId") UUID recipientId);
}
