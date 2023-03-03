package com.yichi.infra.support.mybatis.typehandler;

import java.util.List;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@MappedJdbcTypes({JdbcType.VARCHAR})
@Component
@ConditionalOnExpression("${mybatis.enabled:true}")
public class JsonListTypeHandler extends AbstractJsonObjectTypeHandler<List<?>> {
  public JsonListTypeHandler() {}
}
