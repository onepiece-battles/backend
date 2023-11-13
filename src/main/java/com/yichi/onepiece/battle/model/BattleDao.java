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
  @ResultMap("battleFullView")
  List<BattleFullViewEntity> list(
      @Param("filter") BattleFilter battleFilter,
      @Param("sortList") Collection<BattleSortableField> sortList,
      @Param("userId") UUID userId,
      @Param("externalResourceIds") Collection<UUID> externalResourceIds,
      @Param("offset") int offset,
      @Param("size") int size);

  @Select("SELECT COUNT(id) FROM battle")
  int count();

  @Select("select * from battle where #{characterId} in (battle.starter_id, battle.receiver_id)"
      + "and #{userId} = battle.user_id")
  List<BattleEntity> getBattlesByCharacterId(@Param("characterId") UUID characterId, @Param("userId") UUID userId);


  // result map is defined in battleDao.xml
  // starter_id is mapped to characterEntity who initiate this battle
  // receiver_id is mapped to characterEntity who receive this battle
  @Select("select * from battle where id = #{id}")
  @ResultMap("battleFullView")
  Optional<BattleFullViewEntity> getBattleFullViewEntity(@Param("id") UUID id);
}
