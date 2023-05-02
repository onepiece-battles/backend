package com.yichi.onepiece.Iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Dao
@Mapper
public interface TokenDao extends BaseDao<TokenEntity, UUID> {
    @Select("SELECT * FROM token WHERE id = #{userId}")
    List<TokenEntity> listTokenByUserId(@Param("userId") UUID userId);

    @Select("SELECT * FROM token WHERE token.token_value = #{tokenValue}")
    Optional<TokenEntity> findByTokenValue(@Param("tokenValue") String tokenValue );

    @Update("UPDATE token t SET t.revoked = 'true', t.expired = 'true' where t.user_id = #{userId}")
    void  revokeAllUserTokensByUserId(@Param("userID") UUID userId);
}
