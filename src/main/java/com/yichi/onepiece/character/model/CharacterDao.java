package com.yichi.onepiece.character.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.CharacterFilter;
import com.yichi.onepiece.model.CharacterSortableField;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface CharacterDao extends BaseDao<CharacterEntity, UUID> {
  String FIND_BY_ID = "com.yichi.onepiece.character.model.CharacterDao.findById";
  // see sql definition in characterDao.xml
  List<CharacterEntity> list(
      @Param("characterFilter") CharacterFilter characterFilter,
      @Param("sortList") Collection<CharacterSortableField> sortList,
      @Param("offset") int offset,
      @Param("size") int size,
      @Param("userId") UUID userId,
      // external resource ids means the ids of characters created by other users that are shared to
      // current user.
      @Param("externalResourceIds") Collection<UUID> externalResourceIds);

  @Select("SELECT COUNT(id) FROM `character`")
  int countCharacter();
}
