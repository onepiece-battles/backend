package com.yichi.onepiece.issue.controller;

import com.yichi.onepiece.api.IssueApi;
import com.yichi.onepiece.iam.service.IamService;
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
        ResourceType.ISSUE,
        Set.of(
            Permission.BATCH_UPDATE,
            Permission.UPDATE));
    issueService.batchUpdateIssue(request);
    return ResponseEntity.ok(null);
  }

  @Override
  public ResponseEntity<CreateIssueResponse> createIssue(CreateIssueRequest request) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.ISSUE,
        Set.of(
            Permission.CREATE));
    var i18n = issueService.createIssue(request);
    return ResponseEntity.ok(new CreateIssueResponse().issueCreationResult(i18n));
  }

  @Override
  public ResponseEntity<IssueFullView> getIssueFullView(GetIssueFullViewRequest getIssueFullViewRequest) {
    iamService.checkPermissionOfCurrentAccount(
            ResourceType.ISSUE,
            Set.of(
                    Permission.GET));
    var response = issueService.getIssueFullView(getIssueFullViewRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ListIssueResponse> listIssue(ListIssueRequest request) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.ISSUE, Set.of(Permission.LIST));
    var response = issueService.list(request);
    return ResponseEntity.ok(response);
  }
}
