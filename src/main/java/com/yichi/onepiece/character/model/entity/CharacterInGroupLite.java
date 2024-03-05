package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.GroupPosition;
import com.yichi.onepiece.model.I18nText;
import java.util.UUID;

@Entity
public interface CharacterInGroupLite {
  UUID getId();
  Byte[] getProfileImage();
  I18nText getName();

  GroupPosition getPosition();

  Boolean getOnceBelongedTo();
}
