package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.UserRole;
import java.util.UUID;

@Entity
public interface RoleEntity {
  String ENTITY_TYPE = "ROLE";

  UUID getId();

  UserRole getName();
}
