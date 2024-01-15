package com.yichi.onepiece.group.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
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

  int count(
      @Param("groupFilter") GroupFilter groupFilter,
      @Param("userId") UUID userId,
      @Param("externalResourceIds") Collection<UUID> externalResourceIds);

  @ResultMap("groupFullView")
  @Select("select * from group where id = #{groupId}")
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
}
