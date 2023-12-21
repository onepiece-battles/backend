package com.yichi.onepiece.character.model.dao;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.character.model.entity.CharacterTypeEntity;
import com.yichi.onepiece.model.CharacterType;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface CharacterTypeDao extends BaseDao<CharacterTypeEntity, UUID> {
  @Select(
      "select `character_type` from `character_type` where `character_type`.`character_id` ="
          + " #{characterId}")
  List<CharacterType> listByCharacterId(@Param("characterId") UUID characterId);
}
