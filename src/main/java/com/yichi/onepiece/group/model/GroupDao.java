package com.yichi.onepiece.group.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface GroupDao extends BaseDao<GroupEntity, UUID> {
  @Select(
      "select `group`.*, `character_to_group`.`once_belonged_to`, `character_to_group`.`position` "
      + "from `group` "
      + "left join `character_to_group` on `group`.id = `character_to_group`.`group_id` "
      + "where `character_to_group`.`character_id` = #{characterId}")
  List<CharacterInGroupView> listGroupsByCharacterId(@Param("characterId") UUID characterId);
}
