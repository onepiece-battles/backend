package com.yichi.onepiece.issue.model;

import com.yichi.infra.support.mybatis.dao.BaseDao;
import com.yichi.onepiece.model.IssueFilter;
import com.yichi.onepiece.model.IssueSortableField;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface IssueDao extends BaseDao<IssueEntity, UUID> {

  // sql defined in userDao.xml
  int searchCount(@Param("issueFilter") IssueFilter issueFilter);

  List<IssueListView> listIssues(
      @Param("issueFilter") IssueFilter issueFilter,
      @Param("sortList") Collection<IssueSortableField> sortList,
      @Param("offset") int offset,
      @Param("size") int size);

  // note: next mean the character creates before the current Character
  @Select("select `id` from `issue` where creation_record_id < #{creationRecordId} order by creation_record_id DESC")
  List<UUID> getNextIssueId(
          @Param("creationRecordId") Long currentCreationRecordId);

  // note: previous mean the character creates after the current Character
  @Select("select `id` from `issue` where creation_record_id > #{creationRecordId} order by creation_record_id ASC")
  List<UUID> getPreviousIssueId(
          @Param("creationRecordId") Long currentCreationRecordId);
}
