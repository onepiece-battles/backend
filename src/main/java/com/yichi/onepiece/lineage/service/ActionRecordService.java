package com.yichi.onepiece.lineage.service;

import com.yichi.onepiece.iam.service.RoleService;
import com.yichi.onepiece.lineage.model.ActionRecordDao;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.lineage.model.ActionRecordEntityBuilder;
import com.yichi.onepiece.model.UserRole;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.model.UserEntity;
import io.micrometer.core.annotation.Timed;
import java.time.Clock;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ActionRecordService {
  private final RoleService roleService;
  private final ActionRecordDao actionRecordDao;
  private final Clock clock;

  public ActionRecordService(
      RoleService roleService,
      ActionRecordDao actionRecordDao,
      @Qualifier("onepieceClock") Clock clock) {
    this.roleService = roleService;
    this.actionRecordDao = actionRecordDao;
    this.clock = clock;
  }

  public ActionRecordEntity findOrFail(Long id) {
    return actionRecordDao.findById(id).orElseThrow();
  }

  @Timed
  public ActionRecordEntity create() {
    ActionRecordEntity entity = makeRecord(null);
    actionRecordDao.insert(entity);
    return entity;
  }

  public ActionRecordEntity createWithUser(UserEntity userEntity) {
    var roleEntity = roleService.getRoleByName(userEntity.getLoginRole());
    ActionRecordEntity actionRecordEntity =
        ActionRecordEntityBuilder.newBuilder()
            .setUserId(userEntity.getId())
            .setUsername(userEntity.getUsername())
            .setNickname(userEntity.getNickname())
            .setRoleId(roleEntity.getId())
            .setRoleName(roleEntity.getName())
            .setActionTimestamp(clock.millis())
            .build();
    actionRecordDao.insert(actionRecordEntity);
    return actionRecordEntity;
  }

  public ActionRecordEntity createWithUserAndRole(UserEntity userEntity, UserRole userRole) {
    var roleEntity = roleService.getRoleByName(userRole);
    ActionRecordEntity actionRecordEntity =
            ActionRecordEntityBuilder.newBuilder()
                    .setUserId(userEntity.getId())
                    .setUsername(userEntity.getUsername())
                    .setNickname(userEntity.getNickname())
                    .setRoleId(roleEntity.getId())
                    .setRoleName(roleEntity.getName())
                    .setActionTimestamp(clock.millis())
                    .build();
    actionRecordDao.insert(actionRecordEntity);
    return actionRecordEntity;
  }

  public ActionRecordEntity update(Long id) {
    ActionRecordEntity entity = makeRecord(id);
    actionRecordDao.update(entity);
    return entity;
  }

  public ActionRecordEntity makeRecord(@Nullable Long id) {
    ActionRecordEntityBuilder actionRecordEntityBuilder = ActionRecordEntityBuilder.newBuilder();
    if (id != null) {
      actionRecordEntityBuilder.setId(id);
    }
    var user = UserHelper.getCurrentUserDetail();
    var roleEntity = roleService.getRoleByName(user.getLoginRole());
    actionRecordEntityBuilder
        .setUserId(user.getId())
        .setUsername(user.getUsername())
        .setNickname(user.getNickname())
        .setRoleId(roleEntity.getId())
        .setRoleName(roleEntity.getName())
        .setActionTimestamp(clock.millis());
    return actionRecordEntityBuilder.build();
  }
}
