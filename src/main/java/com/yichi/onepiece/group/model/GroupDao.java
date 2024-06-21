package com.yichi.onepiece.group.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.model.GroupFilter;
import com.yichi.onepiece.model.GroupSortableField;
import com.yichi.onepiece.model.I18nText;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface GroupDao extends BaseDao<GroupEntity, UUID> {
  List<GroupListView> list(
      @Param("groupFilter") GroupFilter groupFilter,
      @Param("sortList") Collection<GroupSortableField> sortList,
      @Param("offset") int offset,
      @Param("size") int size,
      @Param("userId") UUID userId,
      // external resource ids means the ids of characters created by other users that are shared to
      // current user.
      @Param("externalResourceIds") Collection<UUID> externalResourceIds);


  @Select({
          "<script>",
          "SELECT *",
          "FROM `group` ",
          "WHERE `group`.`id` IN ",
          "<foreach item='item' collection='groupIds' open='(' separator=',' close=')'>",
          "  #{item}",
          "</foreach>",
          "</script>"
  })
  List<GroupEntity> listByIds(@Param("groupIds") List<UUID> groupIds);

  @Select("select * from `group` where `group`.`father_group_id` = #{id}")
  List<GroupEntity> listChildGroups(@Param("id") UUID id);

  List<CharacterEntity> listChildCharacters(@Param("id") UUID id);



  // this function list all the father groups of this group, including this group itself.
  @Select({
          "<script>",
          "WITH RECURSIVE `GroupHierarchy` AS (",
          "   SELECT id, father_group_id, 0 AS level",
          "   FROM `group`",
          "   WHERE id = #{id}",
          "   UNION ALL",
          "   SELECT g.id, g.father_group_id, gh.level + 1",
          "   FROM `group` g",
          "   JOIN GroupHierarchy gh ON g.id = gh.father_group_id",
          ")",
          "SELECT * FROM `GroupHierarchy`",
          "</script>"
  })
  List<GroupEntity> listAllFatherGroups(@Param("id") UUID id);

  int count(
      @Param("groupFilter") GroupFilter groupFilter,
      @Param("userId") UUID userId,
      @Param("externalResourceIds") Collection<UUID> externalResourceIds);

  @ResultMap("groupFullView")
  @Select("select * from `group` where id = #{groupId}")
  Optional<GroupFullView> getFullViewById(@Param("groupId") UUID groupId);

  @Select({
    "select `group`.`name` from `group` ",
    "left join `character_to_group` on `group`.`id` = `character_to_group`.`group_id` ",
    "left join `character` on `character`.`id` = `character_to_group`.`character_id` ",
    "where `character`.`id` = #{characterId} and `character_to_group`.`once_belonged_to` = false"
  })
  I18nText getCurrentGroupNameByCharacterId(@Param("characterId") UUID characterId);

  @Select(
      "select `group`.* from `group` left join `character_to_group` on `group`.`id` ="
          + " `character_to_group`.`group_id` where `character_to_group`.`character_id` ="
          + " #{characterId}")
  List<GroupEntity> listGroupsByCharacterId(@Param("characterId") UUID characterId);

  // note: next mean the character creates before the current Character
  @Select("select `id` from `group` where creation_record_id < #{creationRecordId} order by creation_record_id DESC")
  List<UUID> getNextGroupId(@Param("creationRecordId") Long currentCreationRecordId);

  // note: previous mean the character creates after the current Character
  @Select("select `id` from `group` where creation_record_id > #{creationRecordId} order by creation_record_id ASC")
  List<UUID> getPreviousGroupId(@Param("creationRecordId") Long currentCreationRecordId);
}
