package com.yichi.onepiece.user.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.ResourceType;
import java.util.List;
import java.util.UUID;

@Entity
public interface UserResourceEntity {
  UUID getId();

  UUID getUserId();

  UUID getSourceUserId();

  ResourceType getResourceType();

  List<UUID> getResourceIds();
}
