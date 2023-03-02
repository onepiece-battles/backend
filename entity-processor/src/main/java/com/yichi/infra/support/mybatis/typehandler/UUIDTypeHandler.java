package com.yichi.infra.support.mybatis.typehandler;

import com.yichi.infra.support.utils.UUIDUtils;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@MappedJdbcTypes({JdbcType.BINARY})
@Component
@ConditionalOnExpression("${mybatis.enabled:true}")
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {
  public UUIDTypeHandler() {}

  public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setBytes(i, UUIDUtils.toBytes(parameter));
  }

  public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return this.fromBytes(rs.getBytes(columnName));
  }

  public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return this.fromBytes(rs.getBytes(columnIndex));
  }

  public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return this.fromBytes(cs.getBytes(columnIndex));
  }

  private UUID fromBytes(byte[] bytes) {
    return bytes == null ? null : UUIDUtils.fromBytes(bytes);
  }
}
