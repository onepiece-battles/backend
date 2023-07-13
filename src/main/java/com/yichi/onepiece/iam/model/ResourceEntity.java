package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.entity.Entity;
import java.util.Set;
import java.util.UUID;

@Entity
public interface ResourceEntity {
  UUID getId();

  String getName();

  Set<String> getPermissions();
}
