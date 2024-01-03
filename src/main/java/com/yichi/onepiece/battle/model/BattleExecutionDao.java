package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface BattleExecutionDao extends BaseDao<BattleExecutionEntity,UUID> {
  @Select("select * from battle_execution where battle_id = #{battleId}")
  Optional<BattleExecutionEntity> findByBattleId(@Param("battleId") UUID battleId);
}
