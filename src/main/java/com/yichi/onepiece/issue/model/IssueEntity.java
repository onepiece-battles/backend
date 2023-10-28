package com.yichi.onepiece.issue.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.IssueStatus;
import com.yichi.onepiece.model.IssueType;
import java.util.UUID;

@Entity
public interface IssueEntity {
  String ENTITY_TYPE = "ISSUE";

  UUID getId();

  UUID getCreationUserId();

  IssueType getIssueType();

  String getIssueHeader();

  String getIssueDescription();

  IssueStatus getIssueStatus();

  Long getCreationTimestamp();

  Long getModificationTimestamp();

  IssueEntityBuilder toBuilder();
}
