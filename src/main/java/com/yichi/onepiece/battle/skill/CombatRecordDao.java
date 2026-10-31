package com.yichi.onepiece.battle.skill;

import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CombatRecordDao {
  @Insert(
      "INSERT INTO battle_combat_record(battle_id,engine_version,seed,snapshot,created_at)"
          + " VALUES(#{id},#{engine},#{seed},#{snapshot},#{now})")
  void insert(
      @Param("id") UUID id,
      @Param("engine") String engine,
      @Param("seed") long seed,
      @Param("snapshot") String snapshot,
      @Param("now") long now);

  @Select("SELECT snapshot FROM battle_combat_record WHERE battle_id=#{id}")
  Optional<String> snapshot(@Param("id") UUID id);

  @Select("SELECT events FROM battle_combat_record WHERE battle_id=#{id}")
  Optional<String> events(@Param("id") UUID id);

  @Update(
      "UPDATE battle_combat_record SET events=#{events} WHERE battle_id=#{id} AND events IS NULL")
  int complete(@Param("id") UUID id, @Param("events") String events);
}
