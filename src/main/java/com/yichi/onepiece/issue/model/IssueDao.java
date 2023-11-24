package com.yichi.onepiece.issue.model;

import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.IssueFilter;
import com.yichi.onepiece.model.IssueSortableField;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IssueDao extends BaseDao<IssueEntity, UUID> {

  // sql defined in issueDao.xml
  int searchCount(@Param("issueFilter") IssueFilter issueFilter);

  List<IssueEntity> listIssues(
      @Param("issueFilter") IssueFilter issueFilter,
      @Param("sortList") Collection<IssueSortableField> sortList,
      @Param("offset") int offset,
      @Param("size") int size);
}
