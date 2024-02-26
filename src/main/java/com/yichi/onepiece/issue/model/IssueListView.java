package com.yichi.onepiece.issue.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;

@Entity
public interface IssueListView extends IssueEntity {
    ActionRecordEntity getCreationRecord();

    ActionRecordEntity getModificationRecord();
}
