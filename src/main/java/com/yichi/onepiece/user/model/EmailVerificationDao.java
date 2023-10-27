package com.yichi.onepiece.user.model;

import com.yichi.infra.support.mybatis.annotations.Dao;
import com.yichi.infra.support.mybatis.dao.BaseDao;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Dao
@Mapper
public interface EmailVerificationDao extends BaseDao<EmailVerificationEntity, UUID> {
  @Select("select * from email_verification where email_verification.user_id = #{userId} and "
      + "email_verification.email = #{email}")
  Optional<EmailVerificationEntity> getByEmailAndUserId(
      @Param("email") String email,
      @Param("userName") UUID userId
  );
}
