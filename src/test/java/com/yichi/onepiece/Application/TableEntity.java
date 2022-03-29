package com.yichi.onepiece.Application;

public class TableEntity {
  private final String tableName;
  private final String sql;

  public TableEntity(String tableName, String sql) {
    this.tableName = tableName;
    this.sql = sql;
  }

  public String getTableName() {
    return tableName;
  }

  public String getSql() {
    return sql;
  }
}
