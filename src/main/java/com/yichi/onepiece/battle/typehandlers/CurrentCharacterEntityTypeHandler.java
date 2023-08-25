package com.yichi.onepiece.battle.typehandlers;

import com.yichi.infra.support.mybatis.typehandler.AbstractJsonObjectTypeHandler;
import com.yichi.onepiece.battle.model.CurrentCharacterEntity;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.springframework.stereotype.Component;

@MappedJdbcTypes(JdbcType.VARCHAR)
@Component
public class CurrentCharacterEntityTypeHandler
    extends AbstractJsonObjectTypeHandler<CurrentCharacterEntity> {}
