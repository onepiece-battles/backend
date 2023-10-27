package com.yichi.onepiece.user.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface EmailVerificationEntity {
  UUID getId();
  UUID getUserId();
  String getEmail();
  String getEmailVerificationCode();
  Boolean getIsUsed();
  Long getCreationTimestamp();
  Long getExpirationTimestamp();
}
