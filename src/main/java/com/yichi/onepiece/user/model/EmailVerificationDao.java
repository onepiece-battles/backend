package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Dao
@Mapper
public interface EmailVerificationDao extends BaseDao<EmailVerificationEntity, UUID> {
  @Select("select * from email_verification where email_verification.user_id = #{userId} and "
      + "email_verification.email = #{email} and is_used = false and email_verification.email_verification_code = #{verificationCode}")
  Optional<EmailVerificationEntity> getByEmailAndUserId(
      @Param("email") String email,
      @Param("userId") UUID userId,
      @Param("verificationCode") String verificationCode
  );

  @Update("update email_verification set is_used = true where user_id = #{userId} "
      + "and email_verification.email = #{email}")
  void markEmailAsUsed( @Param("userId") UUID userId, @Param("email") String email);
}
