package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;

import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import javax.annotation.Nullable;

@Dao
@Mapper
public interface EmailVerificationDao extends BaseDao<EmailVerificationEntity, UUID> {
  @Update({
      "<script>",
      "UPDATE email_verification SET is_used = TRUE",
      "WHERE email = #{email} AND email_verification_code = #{verificationCode}",
      "AND is_used = FALSE AND expiration_timestamp &gt;= #{now}",
      "<choose><when test='userId == null'>AND user_id IS NULL</when>",
      "<otherwise>AND user_id = #{userId}</otherwise></choose>",
      "</script>"
  })
  int consumeCode(@Param("email") String email, @Param("userId") @Nullable UUID userId,
      @Param("verificationCode") String verificationCode, @Param("now") long now);

  @Select({
      "<script>",
      "SELECT EXISTS(SELECT 1 FROM email_verification",
      "WHERE email = #{email} AND email_verification_code = #{verificationCode}",
      "AND is_used = FALSE AND expiration_timestamp &lt; #{now}",
      "<choose><when test='userId == null'>AND user_id IS NULL</when>",
      "<otherwise>AND user_id = #{userId}</otherwise></choose>)",
      "</script>"
  })
  boolean hasExpiredCode(@Param("email") String email, @Param("userId") @Nullable UUID userId,
      @Param("verificationCode") String verificationCode, @Param("now") long now);

  @Update("update email_verification set is_used = true where user_id is null "
          + "and email_verification.email = #{email}")
  void markEmailAsUsedNullUserId(@Param("email") String email);

  @Update("update email_verification set is_used = true where user_id = #{userId} "
      + "and email_verification.email = #{email}")
  void markEmailAsUsed( @Param("userId") UUID userId, @Param("email") String email);
}
