package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.mybatis.annotations.Column;
import com.yichi.onepiece.model.GroupCategory;
import com.yichi.onepiece.model.I18nText;
import javax.annotation.Nullable;

@Entity
public interface GroupForCharacterView extends CharacterToGroupEntity {
  @Nullable
  Byte[] getProfileImage();
  @Column(json = true)
  I18nText getName();
  GroupCategory getCategory();

  Boolean getSystemResource();

  GroupForCharacterViewBuilder toBuilder();
}
