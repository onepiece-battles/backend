package com.yichi.onepiece.assistant.tools;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.yichi.onepiece.character.helper.CharacterHelper;
import com.yichi.onepiece.character.model.dao.*;
import com.yichi.onepiece.character.model.entity.*;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.fruit.model.*;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import java.util.*;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

/** Protects the original services reused by confirmed writes. */
public class ResourceWriteRegressionTests {
  @Test
  public void characterUpdateKeepsAvatarCreationOwnershipAndSystemFlag() {
    var dao = mock(CharacterDao.class);
    var helper = mock(CharacterHelper.class);
    var records = mock(ActionRecordService.class);
    var fruitService = mock(FruitService.class);
    UUID id = UUID.randomUUID();
    var current =
        CharacterEntityBuilder.newBuilder()
            .setId(id)
            .setCreationRecordId(10L)
            .setModificationRecordId(11L)
            .setImageUri("oss-avatar")
            .setSystemResource(true)
            .setScore(1000)
            .build();
    when(dao.findById(id)).thenReturn(Optional.of(current));
    var modification = mock(ActionRecordEntity.class);
    when(modification.getId()).thenReturn(12L);
    when(records.create()).thenReturn(modification);
    when(fruitService.listFruitEntityByCharacterId(id)).thenReturn(List.of());
    var service =
        new CharacterService(
            dao,
            mock(CharacterToGroupDao.class),
            mock(HakiDao.class),
            fruitService,
            helper,
            mock(CharacterToFruitDao.class),
            records,
            mock(UserService.class));
    try (var constants = mockStatic(UserFacingConstants.class)) {
      var label = new UserFacingConstants.Constant();
      label.setDisplayName(new I18nText().enUS("Success"));
      constants.when(() -> UserFacingConstants.getConstant(anyString())).thenReturn(label);
      service.updateCharacter(new UpdateCharacterRequest().id(id).baseAttack(300).hakis(List.of()));
    }
    var updated = ArgumentCaptor.forClass(CharacterEntity.class);
    verify(helper).calculateCharacterBattleIndex(updated.capture(), eq(List.of()));
    assertThat(updated.getValue().getImageUri()).isEqualTo("oss-avatar");
    assertThat(updated.getValue().getCreationRecordId()).isEqualTo(10);
    assertThat(updated.getValue().getModificationRecordId()).isEqualTo(12);
    assertThat(updated.getValue().getSystemResource()).isTrue();
    assertThat(updated.getValue().getScore()).isEqualTo(1000);
  }

  @Test
  public void fruitUpdateKeepsAvatarCreationOwnershipAndSystemFlag() {
    var dao = mock(FruitDao.class);
    var helper = mock(CharacterHelper.class);
    var records = mock(ActionRecordService.class);
    UUID id = UUID.randomUUID();
    var current =
        FruitEntityBuilder.newBuilder()
            .setId(id)
            .setCreationRecordId(20L)
            .setModificationRecordId(21L)
            .setImageUri("oss-fruit")
            .setSystemResource(true)
            .build();
    when(dao.findById(id)).thenReturn(Optional.of(current));
    var modification = mock(ActionRecordEntity.class);
    when(modification.getId()).thenReturn(22L);
    when(records.create()).thenReturn(modification);
    when(helper.listByFruitId(id)).thenReturn(List.of());
    var service = new FruitService(dao, records, helper, mock(UserService.class));
    try (var constants = mockStatic(UserFacingConstants.class)) {
      var label = new UserFacingConstants.Constant();
      label.setDisplayName(new I18nText().enUS("Success"));
      constants.when(() -> UserFacingConstants.getConstant(anyString())).thenReturn(label);
      service.updateFruit(new UpdateFruitRequest().id(id).attackSlope(1.2));
    }
    var updated = ArgumentCaptor.forClass(FruitEntity.class);
    verify(dao).update(updated.capture());
    assertThat(updated.getValue().getImageUri()).isEqualTo("oss-fruit");
    assertThat(updated.getValue().getCreationRecordId()).isEqualTo(20);
    assertThat(updated.getValue().getModificationRecordId()).isEqualTo(22);
    assertThat(updated.getValue().getSystemResource()).isTrue();
  }

  @Test
  public void eatingSecondFruitRecalculatesUsingBothFruits() {
    var dao = mock(CharacterDao.class);
    var helper = mock(CharacterHelper.class);
    var fruits = mock(FruitService.class);
    UUID id = UUID.randomUUID(), fruit = UUID.randomUUID();
    when(dao.findById(id))
        .thenReturn(
            Optional.of(
                CharacterEntityBuilder.newBuilder().setId(id).setMaximumFruitCount(2).build()));
    var first = FruitEntityBuilder.newBuilder().setId(UUID.randomUUID()).build();
    var second = FruitEntityBuilder.newBuilder().setId(fruit).build();
    when(fruits.listFruitEntityByCharacterId(id)).thenReturn(List.of(first));
    when(fruits.findById(fruit)).thenReturn(second);
    var service =
        new CharacterService(
            dao,
            mock(CharacterToGroupDao.class),
            mock(HakiDao.class),
            fruits,
            helper,
            mock(CharacterToFruitDao.class),
            mock(ActionRecordService.class),
            mock(UserService.class));
    try (var constants = mockStatic(UserFacingConstants.class)) {
      var label = new UserFacingConstants.Constant();
      label.setDisplayName(new I18nText().enUS("Success"));
      constants.when(() -> UserFacingConstants.getConstant(anyString())).thenReturn(label);
      service.eatFruit(id, fruit);
    }
    verify(helper).calculateCharacterBattleIndex(any(), eq(List.of(first, second)));
  }
}
