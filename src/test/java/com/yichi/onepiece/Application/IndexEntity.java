package com.yichi.onepiece.Application;

public class IndexEntity {
  private final String sql;
  private final String constraintName;

  public IndexEntity(String sql, String constraintName) {
    this.sql = sql;
    this.constraintName = constraintName;
  }

  public String getSql() {
    return sql;
  }

  public String getConstraintName() {
    return constraintName;
  }
}
