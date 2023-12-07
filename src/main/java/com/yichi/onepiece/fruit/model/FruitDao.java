package com.yichi.onepiece.fruit.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.FruitFilter;
import com.yichi.onepiece.model.FruitSortableField;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface FruitDao extends BaseDao<FruitEntity, UUID> {

  // details see fruitDao.xml
  List<FruitListView> listFruits(
      @Param("fruitFilter") FruitFilter fruitFilter,
      @Param("sortList") Collection<FruitSortableField> sortList,
      @Param("userId") UUID userId,
      @Param("externalResourceIds") Collection<UUID> externalResourceIds,
      @Param("offset") int offset,
      @Param("size") int size);

  @Select(
        "select `fruit`.* "
            + "from `fruit` "
            + "join `character_to_fruit` on `fruit`.`id` = `character_to_fruit`.`fruit_id` "
            + "where `character_to_fruit`.id = #{characterId} ")
  List<FruitEntity> listByCharacterId(@Param("characterId") UUID characterId);

  int searchCount(
      @Param("userId") UUID userId,
      @Param("fruitFilter") FruitFilter fruitFilter,
      @Param("externalResourceIds") Collection<UUID> externalResourceIds);
}
