package com.yichi.onepiece.User.Model;

import com.yichi.infra.support.entity.Entity;

import java.util.UUID;

@Entity
public interface UserEntity {
    UUID getId();

    String getUserName();

    String getEmail();

    String getOrganization();

    String getEncryptedPassword();

    UserAccountCondition getAccountCondition();

    UserEntityBuilder toBuilder();
}
