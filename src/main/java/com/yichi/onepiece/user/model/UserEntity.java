package com.yichi.onepiece.user.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.OnepieceLocale;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface UserEntity {
  String ENTITY_TYPE = "USER";

  UUID getId();

  String getName();

  String getEmail();

  Boolean getEmailVerified();

  OnepieceLocale getLocale();

  String getOrganization();

  String getLoginRole();

  String getEncryptedPassword();

  UserAccountCondition getAccountCondition();

  @Nullable
  Byte[] getProfileImage();

  UserEntityBuilder toBuilder();
}
