package com.yichi.onepiece.lineage.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.AutoGenIdDao;
import com.yichi.infra.support.mybatis.dao.DeletableDao;
import org.apache.ibatis.annotations.Mapper;

@Dao
@Mapper
public interface ActionRecordDao
    extends AutoGenIdDao<ActionRecordEntity>, DeletableDao<ActionRecordEntity, Long> {
  String FIND_BY_ID = "com.yichi.onepiece.lineage.model.ActionRecordDao.findById";
}
