package com.yichi.onepiece.character.model.dao;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.character.model.entity.CharacterToGroupEntity;
import com.yichi.onepiece.group.model.CharacterInGroupLite;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface CharacterToGroupDao extends BaseDao<CharacterToGroupEntity, UUID> {
  @Select(
      "select `character`.`name` `character_to_group`.`once_belonged_to`,"
          + " `character_to_group`.`position` from `character_to_group`  left join `character` on"
          + " `character`.`id` = `character_to_group`.`character_id` where"
          + " `character_to_group`.`group_id` = #{groupId}")
  List<CharacterInGroupLite> listCharacterInGroupLiteByGroupId(@Param("groupId") UUID groupId);
}
