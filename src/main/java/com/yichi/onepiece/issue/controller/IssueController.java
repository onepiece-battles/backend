package com.yichi.onepiece.issue.controller;

import com.yichi.onepiece.api.IssueApi;
import com.yichi.onepiece.iam.PermissionDefinition;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.issue.model.IssueEntity;
import com.yichi.onepiece.issue.service.IssueService;
import com.yichi.onepiece.model.*;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class IssueController implements IssueApi {

  private final IssueService issueService;
  private final IamService iamService;

  public IssueController(IssueService issueService, IamService iamService) {
    this.issueService = issueService;
    this.iamService = iamService;
  }

  @Override
  public ResponseEntity<Void> batchUpdateIssue(BatchUpdateIssueRequest request) {
    iamService.checkPermissionOfCurrentAccount(
        IssueEntity.ENTITY_TYPE,
        Set.of(
            PermissionDefinition.PermissionEnum.ISSUE_BATCH_UPDATE,
            PermissionDefinition.PermissionEnum.ISSUE_UPDATE));
    issueService.BatchUpdateIssue(request);
    return ResponseEntity.ok(null);
  }

  @Override
  public ResponseEntity<CreateIssueResponse> createIssue(CreateIssueRequest request) {
    iamService.checkPermissionOfCurrentAccount(
        IssueEntity.ENTITY_TYPE, Set.of(PermissionDefinition.PermissionEnum.ISSUE_CREATE));
    var i18n = issueService.createIssue(request);
    return ResponseEntity.ok(new CreateIssueResponse().issueCreationResult(i18n));
  }

  @Override
  public ResponseEntity<ListIssueResponse> listIssue(ListIssueRequest request) {
    iamService.checkPermissionOfCurrentAccount(
        IssueEntity.ENTITY_TYPE, Set.of(PermissionDefinition.PermissionEnum.ISSUE_LIST));
    var response = issueService.list(request);
    return ResponseEntity.ok(response);
  }
}
