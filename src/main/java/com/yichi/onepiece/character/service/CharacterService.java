package com.yichi.onepiece.character.service;

import com.yichi.onepiece.battle.model.BattleConverter;
import com.yichi.onepiece.battle.model.BattleDao;
import com.yichi.onepiece.character.helper.CharacterHelper;
import com.yichi.onepiece.character.model.CharacterDao;
import com.yichi.onepiece.character.model.CharacterEntity;
import com.yichi.onepiece.character.model.CharacterToFruitDao;
import com.yichi.onepiece.character.model.CharacterToFruitEntityBuilder;
import com.yichi.onepiece.character.model.converter.CharacterEntityConverter;
import com.yichi.onepiece.constants.ConstantsContainer;
import com.yichi.onepiece.exception.ExceptionUtils;
import com.yichi.onepiece.fruit.model.converter.FruitConverter;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.image.service.ImageUtil;
import com.yichi.onepiece.lineage.service.ActionRecordService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.model.Character;
import com.yichi.onepiece.user.UserHelper;
import com.yichi.onepiece.user.service.UserService;
import com.yichi.onepiece.user_facing_constants.UserFacingConstants;
import com.yichi.onepiece.user_facing_constants.UserFacingConstantsKey;
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
  private static final ResourceType RESOURCE_TYPE = ResourceType.CHARACTER;

  private final CharacterDao characterDao;
  private final FruitService fruitService;
  // use dao temporarily for cycle dependency
  private final BattleDao battleDao;
  private final CharacterToFruitDao characterToFruitDao;
  private final ActionRecordService actionRecordService;
  private final UserService userService;
  private final CharacterHelper characterHelper;

  public CharacterService(
      CharacterDao CharacterDao,
      FruitService fruitService,
      BattleDao battleDao,
      CharacterToFruitDao characterToFruitDao,
      ActionRecordService actionRecordService,
      UserService userService,
      CharacterHelper characterHelper) {
    this.characterDao = CharacterDao;
    this.fruitService = fruitService;
    this.battleDao = battleDao;
    this.characterToFruitDao = characterToFruitDao;
    this.actionRecordService = actionRecordService;
    this.userService = userService;
    this.characterHelper = characterHelper;
  }

  public Character addCharacter(CreateCharacterRequest request) {
    var characterEntity = CharacterEntityConverter.INSTANCE.toCharacterEntity(request);
    var actionRecord = actionRecordService.create();
    // character does not eat any fruit by default.
    characterEntity =
        characterEntity.toBuilder()
            .setUserId(UserHelper.getCurrentUserId())
            .setProfileImage(
                ArrayUtils.toObject(
                    ImageUtil.loadImageFromDisk(
                        ConstantsContainer.USER_DEFAULT_PROFILE_PATH, "PNG")))
            .setCreationRecordId(actionRecord.getId())
            .setModificationRecordId(actionRecord.getId())
            .build();
    characterDao.insert(characterEntity);
    var insertedEntity =
        characterDao
            .findById(characterEntity.getId())
            .orElseThrow(
                ExceptionUtils.notFound(
                    "Can not find inserted character with id %s", characterEntity.getId()));
    var fruitsInCharacter =
        characterHelper.getFruitInCharacterByCharacterId(characterEntity.getId());
    return CharacterEntityConverter.INSTANCE
        .toCharacter(insertedEntity)
        .fruitInCharacter(fruitsInCharacter);
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
    var characterEntity = characterDao.findById(characterId).orElseThrow();
    var battles =
        battleDao.getBattlesByCharacterId(characterId, UserHelper.getCurrentUserId()).stream()
            .map(BattleConverter.INSTANCE::toBattle)
            .toList();
    var fruits =
        fruitService.listByCharacterId(characterId).stream()
            .map(FruitConverter.INSTANCE::toFruit)
            .toList();
    var character = CharacterEntityConverter.INSTANCE.toCharacter(characterEntity);
    return new CharacterFullView().character(character).battles(battles).fruits(fruits);
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
    var totalSize = characterDao.searchCount(request.getCharacterFilter(), userId, resourceIds);
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
            .map(CharacterEntityConverter.INSTANCE::toCharacter)
            .map(
                character -> {
                  var fruitsInCharacter =
                      characterHelper.getFruitInCharacterByCharacterId(character.getId());
                  return character.fruitInCharacter(fruitsInCharacter);
                })
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
