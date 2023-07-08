package com.yichi.onepiece.Iam.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface RoleEntity {
  UUID getId();

  String getName();
}
