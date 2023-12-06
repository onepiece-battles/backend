package com.yichi.onepiece.battle.model;


import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.UserRole;

@Entity
public interface BattleListView extends BattleEntity{
  Long getCreationTimestamp();
  Long getModificationTimestamp();
  String getStarterName();
  Byte[] getStarterImage();
  String getReceiverName();
  Byte[] getReceiverImage();
  UserRole getRoleName();

  String getCreator();
}
