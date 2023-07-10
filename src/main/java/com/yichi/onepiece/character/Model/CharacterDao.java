package com.yichi.onepiece.character.Model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface CharacterDao extends BaseDao<CharacterEntity, UUID> {
  @Select("SELECT * FROM character_entity LIMIT #{offset}, #{size}")
  List<CharacterEntity> listCharacters(@Param("offset") int offset, @Param("size") int size);

  @Select("SELECT COUNT(id) FROM character_entity")
  int countCharacter();
}
