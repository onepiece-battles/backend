package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.BattleFilter;
import com.yichi.onepiece.model.BattleSortableField;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface BattleDao extends BaseDao<BattleEntity, UUID> {
  // sql defined in battleDao.xml
  List<BattleListView> list(
      @Param("battleFilter") BattleFilter battleFilter,
      @Param("sortList") Collection<BattleSortableField> sortList,
      @Param("userId") UUID userId,
      @Param("externalResourceIds") Collection<UUID> externalResourceIds,
      @Param("offset") int offset,
      @Param("size") int size);

  int searchCount(
      @Param("userId") UUID userId,
      @Param("battleFilter") BattleFilter battleFilter,
      @Param("externalResourceIds") Collection<UUID> externalResourceIds);

  @Select(
      "select * from `view_battle_list_view` as battle where #{characterId} in (battle.starter_id,"
          + " battle.receiver_id)")
  List<BattleListView> listBattlesByCharacterId(@Param("characterId") UUID characterId);

  // result map is defined in battleDao.xml
  // starter_id is mapped to characterEntity who initiate this battle
  // receiver_id is mapped to characterEntity who receive this battle
  @Select("select * from battle where id = #{id}")
  @ResultMap("battleFullView")
  Optional<BattleFullView> getBattleFullView(@Param("id") UUID id);
}
