package com.yichi.infra.support.mybatis.typehandler;

import com.yichi.infra.support.entity.EntitySerializer;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.util.StringUtils;

public abstract class AbstractJsonObjectTypeHandler<T> extends BaseTypeHandler<T> {
  public AbstractJsonObjectTypeHandler() {}

  public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, EntitySerializer.get().serialize(parameter));
  }

  public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return this.fromString(rs.getString(columnName));
  }

  public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return this.fromString(rs.getString(columnIndex));
  }

  public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return this.fromString(cs.getString(columnIndex));
  }

  private T fromString(String content) {
    return StringUtils.isEmpty(content)
        ? null
        : EntitySerializer.get().deserialize(content, (Class<T>) this.getRawType());
  }
}
