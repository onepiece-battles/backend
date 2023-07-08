package com.yichi.onepiece.Iam.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface RoleResourceEntity {
  UUID getId();

  UUID getRoleId();

  UUID getResourceId();
}
