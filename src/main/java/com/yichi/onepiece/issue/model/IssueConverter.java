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

  @Mapping(target = "creationUserId", ignore = true)
  IssueEntity toIssueEntity(Issue issue);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "creationUserId", ignore = true)
  @Mapping(target = "creationTimestamp", ignore = true)
  @Mapping(target = "modificationTimestamp", ignore = true)
  IssueEntity toIssueEntity(CreateIssueRequest request);

  Issue toIssue(IssueEntity issueEntity);
}
