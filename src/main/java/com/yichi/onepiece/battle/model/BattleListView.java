package com.yichi.onepiece.battle.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.mybatis.annotations.Column;
import com.yichi.onepiece.model.BattleReportStage;
import com.yichi.onepiece.model.BattleStatus;
import com.yichi.onepiece.model.I18nText;
import com.yichi.onepiece.model.UserRole;
import java.util.UUID;

@Entity
public interface BattleListView extends BattleEntity {
  Long getCreationTimestamp();

  Long getModificationTimestamp();

  @Column(ignoreUpdates = true, json = true)
  I18nText getStarterName();

  Byte[] getStarterImage();

  @Column(ignoreUpdates = true, json = true)
  I18nText getReceiverName();

  Byte[] getReceiverImage();

  UserRole getRoleName();

  String getCreator();

  BattleStatus getBattleStatus();

  BattleReportStage getBattleReportStage();

  UUID getWinnerId();
}
