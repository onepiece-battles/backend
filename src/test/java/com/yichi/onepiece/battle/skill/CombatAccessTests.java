package com.yichi.onepiece.battle.skill;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.battle.model.BattleDao;
import com.yichi.onepiece.character.model.dao.CharacterDao;
import com.yichi.onepiece.exception.PermissionDeniedException;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.lineage.model.*;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import java.util.*;
import org.testng.annotations.Test;

public class CombatAccessTests {
  @Test
  public void privateSkillsAreReadableOnlyByOwnerOrExplicitShare() {
    var characters = mock(CharacterDao.class);
    var profiles = mock(CombatProfiles.class);
    var users = mock(UserService.class);
    var lineage = mock(ActionRecordDao.class);
    var iam = mock(IamService.class);
    UUID id = UUID.randomUUID(), owner = UUID.randomUUID();
    var actor =
        CombatEngineTests.character(id, "private").toBuilder()
            .setSystemResource(false)
            .setCreationRecordId(9L)
            .build();
    when(characters.findById(id)).thenReturn(Optional.of(actor));
    when(users.getResourceIdsByUserIdResourceType(owner, ResourceType.CHARACTER))
        .thenReturn(List.of());
    var controller =
        new CombatController(
            new CombatCatalog(),
            profiles,
            mock(CombatSession.class),
            mock(CombatProfileDao.class),
            characters,
            mock(BattleDao.class),
            users,
            iam,
            lineage);
    try (var security = mockStatic(UserHelper.class)) {
      security.when(UserHelper::getCurrentUserId).thenReturn(owner);
      when(lineage.findById(9L))
          .thenReturn(
              Optional.of(
                  ActionRecordEntityBuilder.newBuilder().setUserId(UUID.randomUUID()).build()));
      assertThatThrownBy(() -> controller.character(id))
          .isInstanceOf(PermissionDeniedException.class);
      verifyNoInteractions(profiles);
      when(lineage.findById(9L))
          .thenReturn(Optional.of(ActionRecordEntityBuilder.newBuilder().setUserId(owner).build()));
      when(profiles.preview(actor)).thenReturn(CombatCatalog.basic());
      assertThat(controller.character(id)).isEqualTo(CombatCatalog.basic());
      when(lineage.findById(9L)).thenReturn(Optional.empty());
      when(users.getResourceIdsByUserIdResourceType(owner, ResourceType.CHARACTER))
          .thenReturn(List.of(id));
      assertThat(controller.character(id)).isEqualTo(CombatCatalog.basic());
    }
    verify(iam, atLeastOnce())
        .checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of(Permission.GET));
  }

  @Test
  public void publishingRequiresNikaAndAnExplicitReviewedConfirmation() {
    var profiles = mock(CombatProfiles.class);
    var controller =
        new CombatController(
            new CombatCatalog(),
            profiles,
            mock(CombatSession.class),
            mock(CombatProfileDao.class),
            mock(CharacterDao.class),
            mock(BattleDao.class),
            mock(UserService.class),
            mock(IamService.class),
            mock(ActionRecordDao.class));
    try (var security = mockStatic(UserHelper.class)) {
      assertThatThrownBy(
              () ->
                  controller.publish(
                      UUID.randomUUID(),
                      new CombatController.Publish(0, true, CombatCatalog.basic())))
          .isInstanceOf(PermissionDeniedException.class);
      security.when(UserHelper::hasNikaPermission).thenReturn(true);
      assertThatThrownBy(
              () ->
                  controller.publish(
                      UUID.randomUUID(),
                      new CombatController.Publish(0, false, CombatCatalog.basic())))
          .isInstanceOf(IllegalArgumentException.class);
      verifyNoInteractions(profiles);
    }
  }
}
