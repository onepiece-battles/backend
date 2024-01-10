package com.yichi.onepiece.character.model.dao;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.character.model.entity.CharacterInGroupView;
import com.yichi.onepiece.character.model.entity.CharacterToGroupEntity;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface CharacterToGroupDao extends BaseDao<CharacterToGroupEntity, UUID> {

  String CHARACTER_FIELDS = ""
      + "character.name, "
      + "character.profile_image, "
      + "character.system_resource ";
  @Select({
      "select character_to_group.*, ",
      CHARACTER_FIELDS,
      "from character_to_group ",
      "left join `character` on `character`.`id` = character_to_group.character_id ",
      "where character_to_group.group_id = #{groupId}"
  })
  List<CharacterInGroupView> listCharacterInGroupViewByGroupId(@Param("groupId") UUID groupId);
}
