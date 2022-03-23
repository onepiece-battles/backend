package com.yichi.onepiece.Login.Model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface UserEntity {
  UUID getId();

  String getUserName();

  String getOrganization();
}
