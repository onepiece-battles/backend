package com.yichi.onepiece.User.Model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Dao
@Mapper
public interface UserDetailDao extends BaseDao<UserEntity, UUID> {

    @Select("SELECT * FROM user_detail where username=#{username}")
    public Optional<UserDetailImpl> getUserDetailByName(@Param("username") String username);
}
