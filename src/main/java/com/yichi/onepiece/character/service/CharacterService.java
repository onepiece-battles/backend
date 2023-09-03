package com.yichi.onepiece.character.service;

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
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CharacterService {
  Logger logger = LoggerFactory.getLogger(CharacterService.class);
  private static final String EAT_FRUIT_SUCCESS = "EAT FRUIT SUCCESS";
  private static final String NO_MORE_FRUIT = "NO MORE FRUIT CAN BE TAKEN";

  private final CharacterDao characterDao;
  private final FruitService fruitService;
  private final ActionRecordService actionRecordService;

  public CharacterService(
      CharacterDao CharacterDao,
      FruitService fruitService,
      ActionRecordService actionRecordService) {
    this.characterDao = CharacterDao;
    this.fruitService = fruitService;
    this.actionRecordService = actionRecordService;
  }

  public Character addCharacter(AddCharacterRequest request) {
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
            .setHasFruits(false)
            .setFruits(List.of())
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
    return CharacterEntityConverter.INSTANCE.toCharacter(insertedEntity);
  }

  public byte[] updateProfile(UUID characterId, MultipartFile profile) {
    Byte[] profileByte = new Byte[0];
    try {
      profileByte = ArrayUtils.toObject(profile.getInputStream().readAllBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
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
    return ArrayUtils.toPrimitive(updatedCharacter.getProfileImage());
  }

  public byte[] getProfile(UUID characterId) {
    var character =
        characterDao
            .findById(characterId)
            .orElseThrow(ExceptionUtils.notFound("character with id %s not found", characterId));
    return ArrayUtils.toPrimitive(character.getProfileImage());
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
    var characterList =
        characterDao
            .list(
                request.getCharacterFilter(), request.getSortKeys(), offset, request.getPageSize(), UUID.randomUUID(), List.of())
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
