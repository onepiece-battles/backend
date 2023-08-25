package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.BattleFilter;
import com.yichi.onepiece.model.BattleSortableField;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface BattleDao extends BaseDao<BattleEntity, UUID> {
  // sql defined in battleDao.xml
  List<BattleEntity> list(
      @Param("filter")BattleFilter battleFilter,
      @Param("sortList")Collection<BattleSortableField> sortList,
      @Param("offset") int offset,
      @Param("size") int size);

  @Select("SELECT COUNT(id) FROM battle")
  int count();
}
