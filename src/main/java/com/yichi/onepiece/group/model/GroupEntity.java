package com.yichi.onepiece.group.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.mybatis.annotations.Column;
import com.yichi.onepiece.model.GroupCategory;
import com.yichi.onepiece.model.I18nText;
import java.util.UUID;
import javax.annotation.Nullable;

@Entity
public interface GroupEntity {
  UUID getId();

  @Column(json = true)
  I18nText getName();

  GroupCategory getGroupCategory();

  @Nullable
  Byte[] getProfileImage();

  Long getCreationRecordId();

  Long getModificationRecordId();

  UUID getFatherGroupId();

  Boolean getSystemResource();

  GroupEntityBuilder toBuilder();
}
