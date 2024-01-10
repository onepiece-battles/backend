package com.yichi.onepiece.group.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.CharacterSortableField;
import com.yichi.onepiece.model.GroupFilter;
import com.yichi.onepiece.model.GroupSortableField;
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
}
