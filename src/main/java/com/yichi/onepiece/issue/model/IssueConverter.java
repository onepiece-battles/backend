package com.yichi.onepiece.issue.model;

import com.yichi.infra.support.EntityBuilder;
import com.yichi.onepiece.model.CreateIssueRequest;
import com.yichi.onepiece.model.Issue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = EntityBuilder.class)
public interface IssueConverter {
  IssueConverter INSTANCE = Mappers.getMapper(IssueConverter.class);

  @Mapping(target = "creationRecordId", ignore = true)
  @Mapping(target = "modificationRecordId", ignore = true)
  IssueEntity toIssueEntity(Issue issue);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "creationRecordId", ignore = true)
  @Mapping(target = "modificationRecordId", ignore = true)
  IssueEntity toIssueEntity(CreateIssueRequest request);

  com.yichi.onepiece.model.IssueListView toIssue(IssueListView issueEntity);
}
