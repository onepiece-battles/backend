package com.yichi.onepiece.Character.Model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface CharacterBookDao extends BaseDao<CharacterBookEntity, UUID> {
  @Select("SELECT * FROM character_book LIMIT #{offset}, #{size}")
  List<CharacterBookEntity> listCharacters(@Param("offset") int offset, @Param("size") int size);

  @Select("SELECT COUNT(id) FROM character_book")
  int countCharacter();
}
