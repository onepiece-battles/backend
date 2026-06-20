package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Dao
@Mapper
public interface TokenDao extends BaseDao<TokenEntity, UUID> {
  String LIST_TOKENS_BY_USER_ID = "com.yichi.onepiece.iam.model.TokenDao.listTokenByUserId";

  @Select("SELECT * FROM token WHERE user_id = #{userId}")
  List<TokenEntity> listTokenByUserId(@Param("userId") UUID userId);

  @Select("SELECT * FROM token WHERE token.token_value = #{tokenValue}")
  Optional<TokenEntity> findByTokenValue(@Param("tokenValue") String tokenValue);

  @Update("UPDATE token t SET t.revoked = true, t.expired = true where t.user_id = #{userId}")
  void revokeAllUserTokensByUserId(@Param("userId") UUID userId);
}
