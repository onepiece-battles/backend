package com.yichi.onepiece.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.group.model.GroupDao;
import com.yichi.onepiece.group.model.GroupEntity;
import com.yichi.onepiece.group.model.GroupEntityBuilder;
import com.yichi.onepiece.group.model.GroupFullViewBuilder;
import com.yichi.onepiece.lineage.model.ActionRecordEntityBuilder;
import com.yichi.onepiece.model.AddGroupRequest;
import com.yichi.onepiece.model.GetGroupRequest;
import com.yichi.onepiece.model.Group;
import com.yichi.onepiece.model.GroupCategory;
import com.yichi.onepiece.testing.ConcurrencyDatabase;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class GroupHierarchyTests {
  @DataProvider
  public Object[][] cycles() {
    return new Object[][] {{"self"}, {"parent"}, {"ancestor"}, {"existing"}};
  }

  @Test(dataProvider = "cycles")
  public void rejectsCyclesBeforeUpdatingAnyChildren(String cycle) {
    var dao = mock(GroupDao.class);
    var root = UUID.randomUUID();
    var parent = UUID.randomUUID();
    var ancestor = UUID.randomUUID();
    var child = switch (cycle) {
      case "self" -> root;
      case "parent" -> parent;
      default -> ancestor;
    };
    when(dao.findById(root)).thenReturn(Optional.of(group(root, null)));
    if (!"self".equals(cycle)) {
      when(dao.findFatherGroupIdForUpdate(root)).thenReturn(parent);
      when(dao.findFatherGroupIdForUpdate(parent)).thenReturn("existing".equals(cycle) ? root : ancestor);
    }
    var service = new GroupService(dao, null, null, null);
    assertThatThrownBy(() -> service.addGroup(new AddGroupRequest().fatherGroupId(root)
        .childGroupIds(List.of(UUID.randomUUID(), child)))).isInstanceOf(IllegalArgumentException.class);
    verify(dao, never()).update(any());
  }

  @Test
  public void directUpdateCannotBypassHierarchyValidation() {
    var dao = mock(GroupDao.class);
    var root = UUID.randomUUID();
    var descendant = UUID.randomUUID();
    when(dao.findFatherGroupIdForUpdate(descendant)).thenReturn(root);
    var service = new GroupService(dao, null, null, null);
    assertThatThrownBy(() -> service.update(new Group().id(root).fatherGroupId(descendant)))
        .isInstanceOf(IllegalArgumentException.class);
    verify(dao, never()).update(any());
  }

  @Test
  public void validBatchIsDeduplicatedAndFullyValidatedBeforeWriting() {
    var dao = mock(GroupDao.class);
    var root = UUID.randomUUID();
    var first = UUID.randomUUID();
    var second = UUID.randomUUID();
    when(dao.findById(root)).thenReturn(Optional.of(group(root, null)));
    when(dao.listByIds(List.of(first, second))).thenReturn(List.of(group(first, null), group(second, null)));
    try (var constants = mockStatic(UserFacingConstants.class)) {
      constants.when(() -> UserFacingConstants.getConstant(anyString())).thenReturn(new UserFacingConstants.Constant());
      var response = new GroupService(dao, null, null, null).addGroup(new AddGroupRequest().fatherGroupId(root)
          .childGroupIds(List.of(first, first, second)));
      assertThat(response.getResult()).isTrue();
      var changes = ArgumentCaptor.forClass(GroupEntity.class);
      verify(dao, times(2)).update(changes.capture());
      assertThat(changes.getAllValues()).allMatch(entity -> root.equals(entity.getFatherGroupId()));
    }
  }

  @Test
  public void missingChildDoesNotLeavePartialReparenting() {
    var dao = mock(GroupDao.class);
    var root = UUID.randomUUID();
    var existing = UUID.randomUUID();
    var requested = List.of(existing, UUID.randomUUID());
    when(dao.findById(root)).thenReturn(Optional.of(group(root, null)));
    when(dao.listByIds(requested)).thenReturn(List.of(group(existing, null)));
    assertThatThrownBy(() -> new GroupService(dao, null, null, null)
        .addGroup(new AddGroupRequest().fatherGroupId(root).childGroupIds(requested)))
        .isInstanceOf(IllegalArgumentException.class);
    verify(dao, never()).update(any());
  }

  @Test
  public void readingLegacyCycleTerminatesWithoutRepeatingCurrentGroup() {
    var dao = mock(GroupDao.class);
    var root = UUID.randomUUID();
    var parent = UUID.randomUUID();
    var fullView = GroupFullViewBuilder.newBuilder().setId(root).setFatherGroupId(parent)
        .setCreationRecord(ActionRecordEntityBuilder.newBuilder().setId(1L).build()).build();
    when(dao.getFullViewById(root)).thenReturn(Optional.of(fullView));
    when(dao.findById(parent)).thenReturn(Optional.of(group(parent, root)));
    var response = new GroupService(dao, null, null, null).getGroupFullView(new GetGroupRequest().id(root));
    assertThat(response.getFatherGroups()).hasSize(1);
    assertThat(response.getFatherGroups().get(0).getId()).isEqualTo(parent);
    verify(dao).findById(parent);
    verify(dao, never()).findById(root);
  }

  @DataProvider
  public Object[][] updatePaths() {
    return new Object[][] {{false}, {true}};
  }

  @Test(dataProvider = "updatePaths")
  public void concurrentOppositeReparentingCannotCreateCycle(boolean directUpdate) throws Exception {
    try (var database = database()) {
      var first = UUID.randomUUID();
      var second = UUID.randomUUID();
      addGroup(database, first);
      addGroup(database, second);
      var dao = database.mapper(GroupDao.class);
      var firstService = database.transactional(new GroupService(dao, null, null, null), GroupService.class);
      var secondService = database.transactional(new GroupService(dao, null, null, null), GroupService.class);
      var workers = Executors.newFixedThreadPool(2);
      var start = new CountDownLatch(1);
      try {
        var firstResult = workers.submit(() -> reparent(firstService, first, second, false, start));
        var secondResult = workers.submit(() -> reparent(secondService, second, first, directUpdate, start));
        start.countDown();
        assertThat(List.of(firstResult.get(5, TimeUnit.SECONDS), secondResult.get(5, TimeUnit.SECONDS)))
            .containsExactlyInAnyOrder(true, false);
        assertThat(dao.findById(first).orElseThrow().getFatherGroupId() == null
            || dao.findById(second).orElseThrow().getFatherGroupId() == null).isTrue();
      } finally {
        workers.shutdownNow();
      }
    }
  }

  private boolean reparent(GroupService service, UUID parent, UUID child, boolean direct, CountDownLatch start)
      throws Exception {
    start.await();
    try (var constants = mockStatic(UserFacingConstants.class)) {
      constants.when(() -> UserFacingConstants.getConstant(anyString())).thenReturn(new UserFacingConstants.Constant());
      try {
        if (direct) {
          service.update(new Group().id(child).fatherGroupId(parent));
        } else {
          service.addGroup(new AddGroupRequest().fatherGroupId(parent).childGroupIds(List.of(child)));
        }
        return true;
      } catch (IllegalArgumentException expected) {
        return false;
      }
    }
  }

  private GroupEntity group(UUID id, UUID parent) {
    return GroupEntityBuilder.newBuilder().setId(id).setFatherGroupId(parent).setGroupCategory(GroupCategory.GROUP).build();
  }

  private void addGroup(ConcurrencyDatabase database, UUID id) throws Exception {
    try (var statement = database.connection.prepareStatement("INSERT INTO `group` (id, father_group_id, group_category) VALUES (?, NULL, 'GROUP')")) {
      statement.setBytes(1, ConcurrencyDatabase.bytes(id));
      statement.executeUpdate();
    }
  }

  private ConcurrencyDatabase database() throws Exception {
    var database = new ConcurrencyDatabase();
    database.execute("CREATE TABLE `group` (id BINARY(16) PRIMARY KEY, father_group_id BINARY(16), "
        + "group_category VARCHAR(30), name VARCHAR(100), profile_image BLOB, image_uri VARCHAR(255), "
        + "creation_record_id BIGINT, modification_record_id BIGINT, system_resource BOOLEAN)");
    database.migrate("V000000026__serialize_group_hierarchy_writes.sql");
    database.mapperXml("""
        <?xml version="1.0" encoding="UTF-8" ?>
        <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
        <mapper namespace="com.yichi.onepiece.group.model.GroupDao">
          <resultMap id="groupFullView" type="com.yichi.onepiece.group.model.GroupFullView" autoMapping="true"/>
        </mapper>
        """);
    return database;
  }
}
