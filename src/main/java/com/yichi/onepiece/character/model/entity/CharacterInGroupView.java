package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.I18nText;
import javax.annotation.Nullable;

@Entity
public interface CharacterInGroupView extends CharacterToGroupEntity {
  I18nText getName();
  @Nullable
  Byte[] getProfileImage();
  Boolean getSystemResource();

  CharacterInGroupViewBuilder toBuilder();
}
