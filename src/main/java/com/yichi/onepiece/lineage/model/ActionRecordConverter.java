package com.yichi.onepiece.lineage.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.model.ActionRecord;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {EntityBuilder.class})
public interface ActionRecordConverter {
    ActionRecordConverter INSTANCE = Mappers.getMapper(ActionRecordConverter.class);

    ActionRecord toActionRecord(ActionRecordEntity actionRecordEntity);
}
