package com.yichi.onepiece.user.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.AccountStatus;
import com.yichi.onepiece.model.OnepieceLocale;
import com.yichi.onepiece.model.UserRole;
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

  Long getCreationRecordId();

  Long getModificationRecordId();

  String getOrganization();

  UserRole getLoginRole();

  String getEncryptedPassword();

  AccountStatus getAccountStatus();

  @Nullable
  Byte[] getProfileImage();

  UserEntityBuilder toBuilder();
}
