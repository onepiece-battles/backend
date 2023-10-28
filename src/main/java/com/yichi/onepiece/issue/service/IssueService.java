package com.yichi.onepiece.issue.service;

import com.yichi.onepiece.issue.model.IssueConverter;
import com.yichi.onepiece.issue.model.IssueDao;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class IssueService {

  private final IssueDao issueDao;
  private final Clock clock;

  public IssueService(IssueDao issueDao, Clock clock) {
    this.issueDao = issueDao;
    this.clock = clock;
  }

  public void BatchUpdateIssue(BatchUpdateIssueRequest request) {
    request.getUpdatedIssues().stream()
        .map(IssueConverter.INSTANCE::toIssueEntity)
            .map(value -> value.toBuilder()
                    .setModificationTimestamp(clock.millis())
                    .build())
        .forEach(issueDao::update);
  }

  public I18nText createIssue(CreateIssueRequest request) {
    var issueEntity = IssueConverter.INSTANCE.toIssueEntity(request);
    var time = clock.millis();
    issueEntity = issueEntity.toBuilder()
            .setIssueType(request.getIssueType())
            .setIssueStatus(request.getIssueStatus())
        .setCreationUserId(UserHelper.getCurrentUserId())
        .setCreationTimestamp(time)
        .setModificationTimestamp(time)
        .build();
    issueDao.insert(issueEntity);
    return UserFacingConstants.getConstant(UserFacingConstantsKey.ISSUE_CREATION_SUCCESS)
        .getDisplayName();
  }

  public ListIssueResponse list(ListIssueRequest request) {
    var totalSize = issueDao.countIssues();
    // pageIndex is 1- based. offset is 0- based.
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return new ListIssueResponse().issues(List.of()).totalSize(totalSize);
    }
    var issues =
        issueDao
            .listIssues(
                request.getIssueFilter(),
                request.getSortKeys(),
                request.getPageIndex(),
                request.getPageSize())
            .stream()
            .map(IssueConverter.INSTANCE::toIssue)
            .collect(Collectors.toList());
    return new ListIssueResponse().issues(issues).totalSize(totalSize);
  }
}
