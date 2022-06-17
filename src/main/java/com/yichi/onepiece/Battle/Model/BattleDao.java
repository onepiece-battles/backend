package com.yichi.onepiece.Battle.Model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface BattleDao extends BaseDao<BattleEntity, UUID> {
  @Select("select * from battle where action_timestamp > #{action_timestamp}")
  List<BattleEntity> listBattlesByTimestamp(@Param("action_timestamp") Long timestamp);

  @Select("select * from battle")
  List<BattleEntity> listAllBattles(@Param("action_timestamp") Long timestamp);
}
