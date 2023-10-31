package com.yichi.onepiece.character.service;

import com.yichi.onepiece.battle.model.BattleDao;
import com.yichi.onepiece.character.model.CharacterDao;
import com.yichi.onepiece.character.model.CharacterEntity;
import com.yichi.onepiece.character.model.converter.CharacterEntityConverter;
import com.yichi.onepiece.constants.ConstantsContainer;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.image.service.ImageUtil;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.model.Character;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.utils.EncryptHelper;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CharacterService {
  Logger logger = LoggerFactory.getLogger(CharacterService.class);
  private static final String EAT_FRUIT_SUCCESS = "EAT FRUIT SUCCESS";
  private static final String NO_MORE_FRUIT = "NO MORE FRUIT CAN BE TAKEN";
  private static final ResourceType RESOURCE_TYPE = ResourceType.CHARACTER;

  private final CharacterDao characterDao;
  private final FruitService fruitService;
  // use dao temporarily for cycle dependency
  private final BattleDao battleDao;
  private final ActionRecordService actionRecordService;
  private final UserService userService;

  public CharacterService(
      CharacterDao CharacterDao,
      FruitService fruitService,
      BattleDao battleDao, ActionRecordService actionRecordService,
      UserService userService) {
    this.characterDao = CharacterDao;
    this.fruitService = fruitService;
    this.battleDao = battleDao;
    this.actionRecordService = actionRecordService;
    this.userService = userService;
  }

  public Character addCharacter(AddCharacterRequest request) {
    var characterEntity = CharacterEntityConverter.INSTANCE.toCharacterEntity(request);
    var actionRecord = actionRecordService.create();
    var isSystemResource = UserHelper.hasAdminPermission();
    // character does not eat any fruit by default.
    characterEntity =
        characterEntity.toBuilder()
            .setUserId(UserHelper.getCurrentUserId())
            .setProfileImage(
                ArrayUtils.toObject(
                    ImageUtil.loadImageFromDisk(
                        ConstantsContainer.USER_DEFAULT_PROFILE_PATH, "PNG")))
            .setHasFruits(false)
            .setFruits(List.of())
            .setCreationRecordId(actionRecord.getId())
            .setModificationRecordId(actionRecord.getId())
            .setIsSystemResource(isSystemResource)
            .build();
    characterDao.insert(characterEntity);
    var insertedEntity =
        characterDao
            .findById(characterEntity.getId())
            .orElseThrow(
                ExceptionUtils.notFound(
                    "Can not find inserted character with id %s", characterEntity.getId()));
    return CharacterEntityConverter.INSTANCE.toCharacter(insertedEntity);
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

  public CharacterFullView getCharacter(UUID characterId) {
    var characterEntity = characterDao.findById(characterId).orElseThrow();
    logger.info("logging character full view");
    logger.info(characterEntity.toString());
    var battleIds = battleDao.getBattleIdsByCharacterId(characterId, UserHelper.getCurrentUserId());
    var character = CharacterEntityConverter.INSTANCE.toCharacter(characterEntity);
    logger.info(character.toString());
    return new CharacterFullView().character(character).battleIds(battleIds);
  }

  public EatFruitResponse eatFruit(UUID characterId, UUID fruitId) {
    var characterEntityOptional = characterDao.findById(characterId);
    if (characterEntityOptional.isEmpty()) {
      throw new RuntimeException("can not add fruit to character that doesn't exist");
    }
    var characterEntity = characterEntityOptional.get();
    var fruits = characterEntity.getFruits();
    FruitInCharacter newFruit = new FruitInCharacter().fruitId(fruitId).wakeUp(false);
    EatFruitResponse response = new EatFruitResponse();
    if (fruits == null) {
      // this character hasn't taken any fruit yet
      var modificationRecord = actionRecordService.create();
      var characterEntityUpdated =
          characterEntity.toBuilder()
              .setModificationRecordId(modificationRecord.getId())
              .setHasFruits(true)
              .setFruits(List.of(newFruit))
              .build();
      characterDao.update(characterEntityUpdated);
      fruitService.updateOwner(fruitId, characterId);
      response.setFruitMessage(EAT_FRUIT_SUCCESS);
      response.setCharacter(
          CharacterEntityConverter.INSTANCE.toCharacter(
              characterDao.findById(characterId).orElseThrow()));
    } else {
      int fruitLimit = characterEntity.getFruitLimit();
      if (fruits.size() > fruitLimit) {
        // fruit count exceeds the maximum permitted count.
        throw new RuntimeException("fruit count condition error");
      } else if (fruits.size() == fruitLimit) {
        response.setFruitMessage(NO_MORE_FRUIT);
        response.setCharacter(CharacterEntityConverter.INSTANCE.toCharacter(characterEntity));
      } else {
        var modificationRecord = actionRecordService.create();
        fruits.add(newFruit);
        var updatedCharacter =
            characterEntity.toBuilder()
                .setModificationRecordId(modificationRecord.getId())
                .setFruits(fruits)
                .build();
        characterDao.update(updatedCharacter);
        fruitService.updateOwner(fruitId, characterId);
        response.setCharacter(CharacterEntityConverter.INSTANCE.toCharacter(updatedCharacter));
        response.setFruitMessage(EAT_FRUIT_SUCCESS);
      }
    }
    return response;
  }

  public ListCharacterResponse listCharacters(ListCharacterRequest request) {
    var totalSize = characterDao.countCharacter();
    // pageIndex is 1- based. offset is 0- based.
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return new ListCharacterResponse().characters(List.of()).totalSize(totalSize);
    }
    var userId = UserHelper.getCurrentUserId();
    var resourceIds = userService.getResourceIdsByUserIdResourceType(userId, RESOURCE_TYPE);
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
            .map(CharacterEntityConverter.INSTANCE::toCharacter)
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
}
