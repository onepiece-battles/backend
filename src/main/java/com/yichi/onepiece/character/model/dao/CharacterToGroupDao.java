package com.yichi.onepiece.character.model.dao;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.character.model.entity.CharacterInGroupLite;
import com.yichi.onepiece.character.model.entity.CharacterInGroupView;
import com.yichi.onepiece.character.model.entity.CharacterToGroupEntity;
import com.yichi.onepiece.character.model.entity.GroupForCharacterView;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface CharacterToGroupDao extends BaseDao<CharacterToGroupEntity, UUID> {
  String CHARACTER_FIELDS =
          "character.id, "
      + "character.name, "
      + "character.profile_image, "
      + "character.system_resource ";

  String GROUP_FIELDS =
       "group.name, "
      + "group.profile_image, "
      + "group.system_resource, "
      + "group.group_category as category ";

  @Select({
      "select `character_to_group`.`position`, `character_to_group`.`once_belonged_to`, ",
      CHARACTER_FIELDS,
      "from character_to_group ",
      "left join `character` on `character`.`id` = character_to_group.character_id ",
      "where character_to_group.group_id = #{groupId}"
  })
  List<CharacterInGroupView> listCharacterInGroupViewByGroupId(@Param("groupId") UUID groupId);

  @Select({
      "select `character_to_group`.*, ",
      GROUP_FIELDS,
      "from `character_to_group` ",
      "left join `group` on `group`.`id` = `character_to_group`.`group_id` ",
      "where `character_to_group`.`character_id` = #{characterId}"
  })
  List<GroupForCharacterView> listGroupForCharacterViewByCharacterId(@Param("characterId") UUID characterId);

  @Select("select `character_to_group`.`position`, `character_to_group`.`once_belonged_to`, "
      + " `character`.`name`, `character`.`id`, `character`.`profile_image` from `character_to_group` left join `character` "
      + "on character_to_group.character_id = `character`.`id` "
      + "where `character_to_group`.`group_id` = #{groupId}")
  List<CharacterInGroupLite> listCharacterInGroupLiteByGroupId(@Param("groupId") UUID groupId);

  @Select({"select character_to_group.position from character_to_group where character_to_group.character_id = #{characterId} ",
       "and character_to_group.once_belonged_to = false"})
  String getPositionByCharacterId(@Param("characterId")UUID characterId);

  @Select("select * from character_to_group where character_id = #{characterId}")
  List<CharacterToGroupEntity> findByCharacterId(@Param("characterId") UUID characterId);

  @Select({
          "<script>",
          "SELECT * ",
          "FROM `character_to_group` ",
          "WHERE `character_to_group`.`character_id` IN ",
          "<foreach item='item' collection='characterIds' open='(' separator=',' close=')'>",
          "  #{item}",
          "</foreach>",
          "</script>"
  })
  List<CharacterToGroupEntity> listByCharacterIds(@Param("characterIds") List<UUID> characterIds);


  @Select("select * from character_to_group where group_id = #{groupId}")
  List<CharacterToGroupEntity> listByGroupId(@Param("groupId") UUID groupId);
}
