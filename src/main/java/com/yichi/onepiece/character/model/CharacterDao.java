package com.yichi.onepiece.character.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.CharacterFilter;
import com.yichi.onepiece.model.CharacterSortableField;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.swing.text.html.Option;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface CharacterDao extends BaseDao<CharacterEntity, UUID> {
  String FIND_BY_ID = "com.yichi.onepiece.character.model.CharacterDao.findById";

  // see sql definition in characterDao.xml
  List<CharacterListView> list(
      @Param("characterFilter") CharacterFilter characterFilter,
      @Param("sortList") Collection<CharacterSortableField> sortList,
      @Param("offset") int offset,
      @Param("size") int size,
      @Param("userId") UUID userId,
      // external resource ids means the ids of characters created by other users that are shared to
      // current user.
      @Param("externalResourceIds") Collection<UUID> externalResourceIds);

  Optional<CharacterFullView> getFullViewById(@Param("id")UUID id);

  @Select(
      "select * from `character` as c inner join `character_to_fruit` as cf on c.id ="
          + " cf.character_id inner join fruit as f on f.id = cf.fruit_id where f.id = #{fruitId}")
  List<CharacterEntity> listByFruitId(@Param("fruitId") UUID fruitId);

  int searchCount(
      @Param("characterFilter") CharacterFilter characterFilter,
      @Param("userId") UUID userId,
      @Param("externalResourceIds") Collection<UUID> externalResourceIds);
}
