package com.yichi.onepiece.Character.Service;

import com.yichi.onepiece.Character.Model.CharacterDao;
import com.yichi.onepiece.Character.Model.Converter.CharacterEntityConverter;
import com.yichi.onepiece.Exception.ExceptionUtils;
import com.yichi.onepiece.Fruits.Service.FruitService;
import com.yichi.onepiece.model.AddCharacterRequest;
import com.yichi.onepiece.model.Character;
import com.yichi.onepiece.model.EatFruitResponse;
import com.yichi.onepiece.model.FruitInCharacter;
import com.yichi.onepiece.model.ListCharacterRequest;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CharacterService {
  private static final String EAT_FRUIT_SUCCESS = "EAT FRUIT SUCCESS";
  private static final String NO_MORE_FRUIT = "NO MORE FRUIT CAN BE TAKEN";

  private final CharacterDao CharacterDao;
  private final FruitService fruitService;

  public CharacterService(CharacterDao CharacterDao, FruitService fruitService) {
    this.CharacterDao = CharacterDao;
    this.fruitService = fruitService;
  }

  public Character addCharacter(AddCharacterRequest request) {
    var characterEntity = CharacterEntityConverter.INSTANCE.toCharacterEntity(request);
    characterEntity = characterEntity.toBuilder().setHasFruits(false).build();
    CharacterDao.insert(characterEntity);
    var insertedEntity =
        CharacterDao.findById(characterEntity.getId())
            .orElseThrow(
                ExceptionUtils.notFound(
                    "Can not find inserted character with id %s", characterEntity.getId()));
    return CharacterEntityConverter.INSTANCE.toCharacter(insertedEntity);
  }

  public Character addProfile(UUID characterId, MultipartFile profile) {
    Byte[] profileByte = new Byte[0];
    try {
      profileByte = ArrayUtils.toObject(profile.getInputStream().readAllBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }
    var character =
        CharacterDao.findById(characterId)
            .orElseThrow(ExceptionUtils.notFound("character with id %s not found", characterId));
    var updatedCharacter = character.toBuilder().setProfileImage(profileByte).build();
    CharacterDao.insert(updatedCharacter);
    return CharacterEntityConverter.INSTANCE.toCharacter(updatedCharacter);
  }

  public EatFruitResponse eatFruit(UUID characterId, UUID fruitId) {
    var characterEntityOptional = CharacterDao.findById(characterId);
    if (characterEntityOptional.isEmpty()) {
      throw new RuntimeException("can not add fruit to character that doesn't exist");
    }
    var characterEntity = characterEntityOptional.get();
    var fruits = characterEntity.getFruits();
    FruitInCharacter newFruit = new FruitInCharacter().fruitId(fruitId).wakeUp(false);
    EatFruitResponse response = new EatFruitResponse();
    if (fruits == null) {
      // this character hasn't taken any fruit yet
      var characterEntityUpdated =
          characterEntity.toBuilder().setHasFruits(true).setFruits(List.of(newFruit)).build();
      CharacterDao.update(characterEntityUpdated);
      fruitService.updateOwner(fruitId, characterId);
      response.setFruitMessage(EAT_FRUIT_SUCCESS);
      response.setCharacter(
          CharacterEntityConverter.INSTANCE.toCharacter(CharacterDao.findById(characterId).get()));
    } else {
      int fruitLimit = characterEntity.getFruitLimit();
      if (fruits.size() > fruitLimit) {
        // fruit count exceeds the maximum permitted count.
        throw new RuntimeException("fruit count condition error");
      } else if (fruits.size() == fruitLimit) {
        response.setFruitMessage(NO_MORE_FRUIT);
        response.setCharacter(CharacterEntityConverter.INSTANCE.toCharacter(characterEntity));
      } else {
        fruits.add(newFruit);
        var updatedCharacter = characterEntity.toBuilder().setFruits(fruits).build();
        CharacterDao.update(updatedCharacter);
        fruitService.updateOwner(fruitId, characterId);
        response.setCharacter(CharacterEntityConverter.INSTANCE.toCharacter(updatedCharacter));
        response.setFruitMessage(EAT_FRUIT_SUCCESS);
      }
    }
    return response;
  }

  public List<Character> listCharacters(ListCharacterRequest request) {
    var totalSize = CharacterDao.countCharacter();
    // pageIndex is 1- based. offset is 0- based.
    var offset = (request.getPageIndex() - 1) * request.getPageSize();
    if (offset >= totalSize || offset < 0) {
      return List.of();
    }

    return CharacterDao.listCharacters(offset, request.getPageSize()).stream()
        .map(CharacterEntityConverter.INSTANCE::toCharacter)
        .collect(Collectors.toList());
  }
}
