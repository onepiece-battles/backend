package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface RoleToResourcePermissionEntity {
    UUID getId();
    UUID getRoleId();
    UUID getResourcePermissionId();
}
