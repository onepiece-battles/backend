package com.yichi.infra.support.mybatis.typehandler;

import com.yichi.onepiece.model.CharacterTypeInfo;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@MappedJdbcTypes({JdbcType.VARCHAR})
@Component
@ConditionalOnExpression("${mybatis.enabled:true}")
public class CharacterTypeInfoTypeHandler extends AbstractJsonObjectTypeHandler<CharacterTypeInfo> {
  public CharacterTypeInfoTypeHandler() {}
}
