package com.yichi.onepiece.configuration.TpeHandlers;

import com.yichi.infra.support.mybatis.typehandler.AbstractJsonObjectTypeHandler;
import com.yichi.onepiece.model.I18nText;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@MappedJdbcTypes({JdbcType.VARCHAR})
@Component
@ConditionalOnExpression("${mybatis.enabled:true}")
public class I18nTypeHandler extends AbstractJsonObjectTypeHandler<I18nText> {}
