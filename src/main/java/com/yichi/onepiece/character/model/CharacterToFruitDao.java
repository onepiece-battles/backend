package com.yichi.onepiece.character.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface CharacterToFruitDao extends BaseDao<CharacterToFruitEntity, UUID> {

  @Select("select * from character_to_fruit where character_to_fruit.character_id = #{characterId}")
  List<CharacterToFruitEntity> listByCharacterId(@Param("characterId") UUID characterId);
}
