package com.yichi.onepiece.configuration.TpeHandlers;

import com.yichi.infra.support.mybatis.typehandler.AbstractJsonObjectTypeHandler;
import com.yichi.onepiece.model.LanguageCode;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@MappedJdbcTypes({JdbcType.VARCHAR})
@Component
@ConditionalOnExpression("${mybatis.enabled:true}")
public class LanguageCodeTypeHandler extends AbstractJsonObjectTypeHandler<LanguageCode> {}
