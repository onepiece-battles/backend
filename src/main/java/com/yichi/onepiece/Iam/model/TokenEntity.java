package com.yichi.onepiece.Iam.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.User.Model.UserEntity;

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
