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
public interface BattleStatusDetailDao extends BaseDao<BattleStatusDetailEntity, UUID> {

  @Select("SELECT * FROM battle_status_detail WHERE battle_id=#{battleId}")
  List<BattleStatusDetailEntity> listByBattleId(@Param("battleId")UUID battleId);
}
