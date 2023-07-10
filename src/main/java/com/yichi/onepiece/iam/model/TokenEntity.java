package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface TokenEntity {
  UUID getId();

  String getTokenValue();

  TokenType getTokenType();

  Boolean getRevoked();

  Boolean getExpired();

  UUID getUserId();
}
