package com.yichi.onepiece.Fruits.Model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

@Dao
@Mapper
public interface FruitDao extends BaseDao<FruitEntity, UUID> {

    @Select("SELECT * FROM fruit LIMIT #{offset}, #{size}")
    List<FruitEntity> listFruits(@Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(id) FROM fruit")
    int countFruit();
}
