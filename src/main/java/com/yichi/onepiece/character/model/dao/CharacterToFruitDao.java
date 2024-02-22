package com.yichi.onepiece.character.model.dao;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.character.model.entity.CharacterInFruitView;
import com.yichi.onepiece.character.model.entity.CharacterToFruitEntity;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface CharacterToFruitDao extends BaseDao<CharacterToFruitEntity, UUID> {

  String CHARACTER_IN_FRUIT_PROPERTIES =
          "`character`.`name`, `character`.`profile_image`, `character`.`bounty`, `character`.`group_gain`, " +
          "`character`.`attack`, `character`.`defense`, `character`.`intelligence`, `character`.`blood`, " +
          "`character`.`speed`, `character`.`energy` ";


  @Select("select * from character_to_fruit where character_to_fruit.character_id = #{characterId}")
  List<CharacterToFruitEntity> listByCharacterId(@Param("characterId") UUID characterId);

  @Select("select character_to_fruit.*, " +
          CHARACTER_IN_FRUIT_PROPERTIES +
          "from character_to_fruit " +
          "left join `character` on `character_to_fruit`.`character_id` = `character`.`id` " +
          "where `character_to_fruit`.`fruit_id` = #{fruitId} ")
  List<CharacterInFruitView> listCharacterInFruitViewByFruitId(@Param("fruitId") UUID fruitId);
}
