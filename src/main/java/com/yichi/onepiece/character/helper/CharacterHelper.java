package com.yichi.onepiece.character.helper;

import com.yichi.onepiece.character.model.CharacterDao;
import com.yichi.onepiece.character.model.CharacterEntity;
import com.yichi.onepiece.character.model.CharacterToFruitDao;
import com.yichi.onepiece.model.FruitInCharacter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CharacterHelper {
  private final CharacterDao characterDao;
  private final CharacterToFruitDao characterToFruitDao;

  public CharacterHelper(CharacterDao characterDao, CharacterToFruitDao characterToFruitDao) {
    this.characterDao = characterDao;
    this.characterToFruitDao = characterToFruitDao;
  }

  // list all fruit holders of certain fruit
  public List<CharacterEntity> listByFruitId(UUID fruitId) {
    return characterDao.listByFruitId(fruitId);
  }

  public List<FruitInCharacter> getFruitInCharacterByCharacterId(UUID characterId) {
    return characterToFruitDao.listByCharacterId(characterId).stream().map(
        characterToFruitEntity -> new FruitInCharacter().fruitId(characterToFruitEntity.getFruitId()).wakeUp(characterToFruitEntity.getWakeUp())
    ).collect(Collectors.toList());
  }
}
