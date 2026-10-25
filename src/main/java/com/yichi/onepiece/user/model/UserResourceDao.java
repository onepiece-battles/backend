package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.ResourceType;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface UserResourceDao extends BaseDao<UserResourceEntity, UUID> {

  @Select(
      "SELECT resource_id FROM user_resource WHERE user_id = #{userId} AND resource_type ="
          + " #{resourceType}")
  List<UUID> getResourceIdsByUserIdAndResourceType(
      @Param("userId") UUID userId, @Param("resourceType") ResourceType resourceType);

  @Select({
      "<script>",
      "SELECT COUNT(*) FROM",
      "<choose>",
      "<when test=\"resourceType != null and resourceType.name() == 'CHARACTER'\">`character`</when>",
      "<when test=\"resourceType != null and resourceType.name() == 'FRUIT'\">`fruit`</when>",
      "<when test=\"resourceType != null and resourceType.name() == 'BATTLE'\">`battle`</when>",
      "<when test=\"resourceType != null and resourceType.name() == 'GROUP'\">`group`</when>",
      "<otherwise>(SELECT NULL AS id, NULL AS creation_record_id, FALSE AS system_resource)</otherwise>",
      "</choose> resource",
      "INNER JOIN `action_lineage` creation ON resource.creation_record_id = creation.id",
      "WHERE (#{admin} = TRUE OR (creation.user_id = #{userId} AND resource.system_resource = FALSE))",
      "<choose>",
      "<when test=\"resourceIds != null and !resourceIds.isEmpty()\">",
      "AND resource.id IN",
      "<foreach item='resourceId' collection='resourceIds' open='(' separator=',' close=')'>",
      "#{resourceId}",
      "</foreach>",
      "</when>",
      "<otherwise>AND 1 = 0</otherwise>",
      "</choose>",
      "</script>"
  })
  int countShareableResources(
      @Param("userId") UUID userId,
      @Param("resourceType") ResourceType resourceType,
      @Param("resourceIds") List<UUID> resourceIds,
      @Param("admin") boolean admin);
}
