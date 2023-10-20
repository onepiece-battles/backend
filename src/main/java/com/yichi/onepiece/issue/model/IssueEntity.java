package com.yichi.onepiece.issue.model;

import com.yichi.infra.support.entity.Entity;
import java.util.UUID;

@Entity
public interface IssueEntity {
  UUID getId();
}
