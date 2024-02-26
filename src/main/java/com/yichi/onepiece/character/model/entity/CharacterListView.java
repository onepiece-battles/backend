package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.model.Haki;
import java.util.List;

@Entity
public interface CharacterListView extends CharacterEntity {
  ActionRecordEntity getCreationRecord();
  ActionRecordEntity getModificationRecord();
  List<Haki> getHakis();
}
