package com.yichi.onepiece.character.service;

import com.yichi.onepiece.character.model.converter.CharacterEntityConverter;
import com.yichi.onepiece.character.model.dao.CharacterDao;
import com.yichi.onepiece.character.model.dao.CharacterToFruitDao;
import com.yichi.onepiece.character.model.dao.CharacterToGroupDao;
import com.yichi.onepiece.character.model.dao.HakiDao;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import com.yichi.onepiece.character.model.entity.CharacterToFruitEntityBuilder;
import com.yichi.onepiece.character.model.entity.CharacterToGroupEntity;
import com.yichi.onepiece.character.model.entity.CharacterToGroupEntityBuilder;
import com.yichi.onepiece.character.model.entity.HakiEntityBuilder;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.model.Character;
import com.yichi.onepiece.model.CharacterFullView;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
import com.yichi.onepiece.utils.EncryptHelper;
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CharacterService {
  Logger logger = LoggerFactory.getLogger(CharacterService.class);
  private static final ResourceType RESOURCE_TYPE = ResourceType.CHARACTER;

  private final CharacterDao characterDao;
  private final CharacterToGroupDao characterToGroupDao;
  private final HakiDao hakiDao;
  private final FruitService fruitService;

  private final CharacterToFruitDao characterToFruitDao;
  private final ActionRecordService actionRecordService;
  private final UserService userService;

  public CharacterService(
      CharacterDao CharacterDao,
      CharacterToGroupDao characterToGroupDao, HakiDao hakiDao,
      FruitService fruitService,
      CharacterToFruitDao characterToFruitDao,
      ActionRecordService actionRecordService,
      UserService userService) {
    this.characterDao = CharacterDao;
    this.characterToGroupDao = characterToGroupDao;
    this.hakiDao = hakiDao;
    this.fruitService = fruitService;
    this.characterToFruitDao = characterToFruitDao;
    this.actionRecordService = actionRecordService;
    this.userService = userService;
  }

  public Character addCharacter(CreateCharacterRequest request) {
    var characterEntity = CharacterEntityConverter.INSTANCE.toCharacterEntity(request);
    var actionRecord = actionRecordService.create();
    // character does not eat any fruit by default.
    characterEntity =
        characterEntity.toBuilder()
            .setCreationRecordId(actionRecord.getId())
            .setModificationRecordId(actionRecord.getId())
            .setSystemResource(UserHelper.hasNikaPermission())
            .build();
    characterDao.insert(characterEntity);
    createHakis(request.getHakis(), characterEntity.getId());
    var insertedEntity =
        characterDao
            .findById(characterEntity.getId())
            .orElseThrow(
                ExceptionUtils.notFound(
                    "Can not find inserted character with id %s", characterEntity.getId()));
    return CharacterEntityConverter.INSTANCE.toCharacter(insertedEntity);
  }

  private void createHakis(List<Haki> hakis, UUID characterId) {
    if (hakis.isEmpty()) {
      return;
    }
    var hakiEntities =
        hakis.stream()
            .map(
                haki ->
                    HakiEntityBuilder.newBuilder()
                        .setHaki(haki)
                        .setCharacterId(characterId)
                        .build())
            .toList();
    hakiDao.batchInsert(hakiEntities);
  }

  public String updateProfile(UUID characterId, String profile) {
    Byte[] profileByte = ArrayUtils.toObject(EncryptHelper.base64Decode(profile));
    var modificationRecord = actionRecordService.create();
    var character =
        characterDao
            .findById(characterId)
            .orElseThrow(ExceptionUtils.notFound("character with id %s not found", characterId));
    var updatedCharacter =
        character.toBuilder()
            .setModificationRecordId(modificationRecord.getId())
            .setProfileImage(profileByte)
            .build();
    characterDao.update(updatedCharacter);
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(updatedCharacter.getProfileImage()));
  }

  public String getProfile(UUID characterId) {
    var character =
        characterDao
            .findById(characterId)
            .orElseThrow(ExceptionUtils.notFound("character with id %s not found", characterId));
    return EncryptHelper.base64Encode(ArrayUtils.toPrimitive(character.getProfileImage()));
  }

  public CharacterFullView getCharacterFullView(UUID characterId) {
    var characterFullView = characterDao.getFullViewById(characterId).orElseThrow();
    return CharacterEntityConverter.INSTANCE.toCharacterFullView(characterFullView);
  }

  /**
   * This function execute the request for a character to join a group.
   * <li> First, mark all groups related to this character not in unless the group id from request matches </li>
   * <li> Insert new group relation if new group not found. </li>
   *
   * @param request the request for a character to join a group.
   * @return Whether this character successfully joined this group.
   */
  public JoinGroupResponse joinGroup(JoinGroupRequest request) {
    var characterToGroups = characterToGroupDao.findByCharacterId(request.getCharacterId());
    var currentGroup = characterToGroups.stream()
        .filter(characterToGroupEntity -> !characterToGroupEntity.getOnceBelongedTo()).findFirst();
    // exit current group if present
    currentGroup.ifPresent(characterToGroupEntity -> characterToGroupDao.update(
        characterToGroupEntity.toBuilder().setOnceBelongedTo(true).build()));
    // update original groups
    var groupMatchEntity = characterToGroups
        .stream()
        .filter(characterToGroupEntity ->
           characterToGroupEntity.getGroupId() == request.getGroupId()
          ).findFirst();
    if (groupMatchEntity.isPresent()) {
      characterToGroupDao.update(groupMatchEntity.get().toBuilder().setOnceBelongedTo(false).build());
    } else {
      CharacterToGroupEntity characterToGroupEntity = CharacterToGroupEntityBuilder.newBuilder()
          .setCharacterId(request.getCharacterId())
          .setGroupId(request.getGroupId())
          .onceBelongedTo(false)
          .build();
      characterToGroupDao.insert(characterToGroupEntity);
    }
    return new JoinGroupResponse()
        .result(UserFacingConstants.getConstant(UserFacingConstantsKey.GROUP_JOIN_SUCCESS)
        .getDisplayName());
  }

  public EatFruitResponse eatFruit(UUID characterId, UUID fruitId) {
    var characterEntityOptional = characterDao.findById(characterId);
    if (characterEntityOptional.isEmpty()) {
      throw new RuntimeException("can not add fruit to character that doesn't exist");
    }
    var characterEntity = characterEntityOptional.get();
    int fruitLimit = characterEntity.getMaximumFruitCount();
    var fruits = fruitService.listByCharacterId(characterId);
    EatFruitResponse response = new EatFruitResponse();
    if (fruits.size() == fruitLimit) {
      // fruit count exceeds the maximum permitted count.
      return response.fruitMessage(
          UserFacingConstants.getConstant(UserFacingConstantsKey.FRUIT_COUNT_REACH_LIMIT)
              .getDisplayName());
    }

    // this character can take more fruit
    var characterToFruitEntity =
        CharacterToFruitEntityBuilder.newBuilder()
            .setCharacterId(characterId)
            .setFruitId(fruitId)
            .setWakeUp(false)
            .build();
    characterToFruitDao.insert(characterToFruitEntity);
    return response.fruitMessage(
        UserFacingConstants.getConstant(UserFacingConstantsKey.FRUIT_EATEN_SUCCESS)
            .getDisplayName());
  }

  public ListCharacterResponse listCharacters(ListCharacterRequest request) {
    var userId = UserHelper.getCurrentUserId();
    var resourceIds = userService.getResourceIdsByUserIdResourceType(userId, RESOURCE_TYPE);
    var totalSize = characterDao.count(request.getCharacterFilter(), userId, resourceIds);
    logger.info("count is {}", totalSize);
    // pageIndex is 1- based. offset is 0- based.
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return new ListCharacterResponse().characters(List.of()).totalSize(totalSize);
    }
    var characterList =
        characterDao
            .list(
                request.getCharacterFilter(),
                request.getSortKeys(),
                offset,
                request.getPageSize(),
                userId,
                resourceIds)
            .stream()
            .map(CharacterEntityConverter.INSTANCE::toCharacterListView)
            .toList();
    return new ListCharacterResponse().characters(characterList).totalSize(totalSize);
  }

  public CharacterEntity getCharacterEntityById(UUID characterId) {
    return characterDao
        .findById(characterId)
        .orElseThrow(ExceptionUtils.notFound("Character with id: %s not found", characterId));
  }

  public CharacterEntity findByCharacterId(UUID characterId) {
    return characterDao.findById(characterId).orElseThrow();
  }

  public UpdateCharacterResponse updateCharacter(Character character) {
    characterDao.update(CharacterEntityConverter.INSTANCE.toCharacterEntity(character));
    logger.info("the date is: {}", character.getDateOfBirth().toString());
    var response = new UpdateCharacterResponse();
    return response.result(
            UserFacingConstants.getConstant(UserFacingConstantsKey.UPDATE_SUCCESS)
                    .getDisplayName()).success(Boolean.TRUE);
  }
}
