package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.entity.Entity;
import java.util.List;
import java.util.UUID;

@Entity
public interface RoleResourceEntity {
  UUID getId();

  UUID getRoleId();

  UUID getResourceId();

  List<String> getPermissions();
}
