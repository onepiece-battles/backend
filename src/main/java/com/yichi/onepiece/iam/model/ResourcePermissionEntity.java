package com.yichi.onepiece.iam.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.iam.PermissionDefinition;
import com.yichi.onepiece.model.Permission;
import com.yichi.onepiece.model.ResourceType;

import java.util.UUID;

@Entity
public interface ResourcePermissionEntity {
    UUID getId();
    ResourceType getResourceType();
    Permission getPermission();
}
