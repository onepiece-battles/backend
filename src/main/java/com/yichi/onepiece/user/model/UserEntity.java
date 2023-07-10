package com.yichi.onepiece.user.model;

import com.yichi.infra.support.entity.Entity;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface UserEntity {
  UUID getId();

  String getName();

  String getEmail();

  String getOrganization();

  String getEncryptedPassword();

  UserAccountCondition getAccountCondition();

  List<String> getUserRoles();

  @Nullable
  Byte[] getProfileImage();

  UserEntityBuilder toBuilder();
}
