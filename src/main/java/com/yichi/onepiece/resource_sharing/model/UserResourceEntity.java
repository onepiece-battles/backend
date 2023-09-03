package com.yichi.onepiece.resource_sharing.model;


import com.yichi.infra.support.mybatis.annotations.Dao;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.flywaydb.core.internal.resource.ResourceType;

@Dao
@Mapper
public interface UserResourceEntity {
  UUID getId();
  UUID getUserId();
  ResourceType getResourceType();
  UUID getResourceId();
}
