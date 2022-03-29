package com.yichi.onepiece.Application;

import com.yichi.infra.support.mybatis.annotations.Dao;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
public interface SchemaMapper {
  /**
   * List all tables with their DDLs
   *
   * <p>Note: This method is compatible with H2 only.
   *
   * @param schema TABLE_SCHEMA
   * @return list of TableEntity.
   */
  @Select(
      value = {
        "SELECT TABLE_NAME AS table_name, ",
        "SQL AS sql ",
        "FROM INFORMATION_SCHEMA.TABLES ",
        "WHERE TABLE_SCHEMA=#{schema} ",
        "ORDER BY TABLE_NAME"
      })
  List<TableEntity> showTables(@Param("schema") String schema);

  /**
   * List all indexes with their DDLs on a table.
   *
   * <p>Note: This method is compatible with H2 only.
   *
   * @param schema TABLE_SCHEMA
   * @param table table name
   * @return list of IndexEntity
   */
  @Select(
      value = {
        "SELECT SQL AS sql, ",
        "CONSTRAINT_NAME AS constraint_name ",
        "FROM INFORMATION_SCHEMA.INDEXES ",
        "WHERE TABLE_SCHEMA=#{schema} AND TABLE_NAME=#{table} ORDER BY INDEX_NAME"
      })
  List<IndexEntity> showIndexes(@Param("schema") String schema, @Param("table") String table);

  /**
   * List all constraints with their DDLs on a table.
   *
   * <p>Note: This method is compatible with H2 only.
   *
   * @param schema TABLE_SCHEMA
   * @param table table name
   * @return list of ConstraintEntity.
   */
  @Select(
      value = {
        "SELECT SQL AS sql ",
        "FROM INFORMATION_SCHEMA.CONSTRAINTS ",
        "WHERE TABLE_SCHEMA=#{schema} AND TABLE_NAME=#{table} ORDER BY CONSTRAINT_NAME"
      })
  List<String> showConstraints(@Param("schema") String schema, @Param("table") String table);
}
