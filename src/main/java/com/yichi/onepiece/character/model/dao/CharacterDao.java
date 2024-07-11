package com.yichi.onepiece.character.model.dao;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.character.model.entity.CharacterFullView;
import com.yichi.onepiece.character.model.entity.CharacterListView;
import com.yichi.onepiece.model.CharacterFilter;
import com.yichi.onepiece.model.CharacterSortableField;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.*;

@Dao
@Mapper
public interface CharacterDao extends BaseDao<CharacterEntity, UUID> {
  String FIND_BY_ID = "com.yichi.onepiece.character.model.dao.CharacterDao.findById";

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



  // note: next mean the character creates before the current Character
  @Select("select `id` from `character` where creation_record_id < #{creationRecordId} order by creation_record_id DESC")
  List<UUID> getNextCharacterId(
          @Param("creationRecordId") Long currentCharacterCreationRecordId);

  // note: previous mean the character creates after the current Character
  @Select("select `id` from `character` where creation_record_id > #{creationRecordId} order by creation_record_id ASC")
  List<UUID> getPreviousCharacterId(
          @Param("creationRecordId") Long currentCharacterCreationRecordId);

  // see sql definition in characterDao.xml
  Optional<CharacterFullView> getFullViewById(@Param("id") UUID id);

  @Select(
      "select * from `character` as c inner join `character_to_fruit` as cf on c.id ="
          + " cf.character_id inner join fruit as f on f.id = cf.fruit_id where f.id = #{fruitId}")
  List<CharacterEntity> listByFruitId(@Param("fruitId") UUID fruitId);

  int count(
      @Param("characterFilter") CharacterFilter characterFilter,
      @Param("userId") UUID userId,
      @Param("externalResourceIds") Collection<UUID> externalResourceIds);

  List<CharacterListView> listCharactersByGroupId(@Param("groupId") UUID groupId);


  @Select("select * from `character` where `character`.`image_uri` is null")
  List<CharacterEntity> listNoUriCharacters();
}
