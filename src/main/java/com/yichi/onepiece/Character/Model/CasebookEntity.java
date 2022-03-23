package com.yichi.onepiece.Character.Model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface CasebookEntity {
  UUID getCasebookId();

  Long getCasebookNumber();
}
