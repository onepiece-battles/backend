package com.yichi.onepiece.character.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.Haki;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface HakiDao extends BaseDao<UUID, HakiEntity> {
  @Select("select `haki` from `haki` where `haki`.`character_id` = #{characterId}")
  List<Haki> listByCharacterId(@Param("characterId") UUID characterId);
}
