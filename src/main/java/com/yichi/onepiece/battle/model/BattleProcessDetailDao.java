package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface BattleProcessDetailDao extends BaseDao<BattleProcessDetailEntity, UUID> {

  @Select("SELECT * FROM battle_process_detail WHERE battle_id = #{battleId} order by battle_process_round_number ASC")
  List<BattleProcessDetailEntity> listByBattleId(@Param("battleId") UUID battleId);
}
