package com.yichi.onepiece.fruit.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.UserRole;

@Entity
public interface FruitListView extends FruitEntity {
  Long getCreationTimestamp();
  Long getModificationTimestamp();
  UserRole getRoleName();
  String getCreator();
}
