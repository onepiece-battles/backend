package com.yichi.onepiece.battle.skill;

import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CombatProfileDao {
  @Select(
      "SELECT payload FROM character_combat_profile WHERE character_id=#{id} ORDER BY version DESC"
          + " LIMIT 1")
  Optional<String> latest(@Param("id") UUID id);

  @Select("SELECT payload FROM character_combat_profile WHERE character_id=#{id} ORDER BY version")
  List<String> history(@Param("id") UUID id);

  @Select("SELECT id FROM `character` WHERE id=#{id} FOR UPDATE")
  UUID lockCharacter(@Param("id") UUID id);

  @Insert(
      "INSERT INTO character_combat_profile(character_id,version,payload,created_at)"
          + " VALUES(#{id},#{version},#{payload},#{now})")
  void insert(
      @Param("id") UUID id,
      @Param("version") int version,
      @Param("payload") String payload,
      @Param("now") long now);

  @Select("SELECT id FROM `character` WHERE system_resource=TRUE ORDER BY creation_record_id")
  List<UUID> systemCharacters();
}
