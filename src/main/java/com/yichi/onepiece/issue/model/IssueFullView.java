package com.yichi.onepiece.issue.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.user.model.UserEntity;

@Entity
public interface IssueFullView extends IssueListView {
    UserEntity getIssueCreator();
    IssueEntity getPreviousIssue();
    IssueEntity getNextIssue();

    IssueFullViewBuilder toBuilder();
}
