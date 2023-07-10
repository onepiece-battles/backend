package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface UserRoleEntity {
  UUID getId();

  UUID getUserId();

  UUID getRoleId();
}
