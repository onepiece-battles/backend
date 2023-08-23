package com.yichi.onepiece.lineage.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.mybatis.annotations.Table;
import java.util.UUID;

@Entity
@Table(name = "action_lineage")
public interface ActionRecordEntity {
  Long getId();

  Long getActionTimestamp();

  UUID getUserId();

  String getUserName();

  UUID getRoleId();

  String getRoleName();
}
