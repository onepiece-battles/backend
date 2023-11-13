package com.yichi.onepiece.character.helper;

import com.yichi.onepiece.character.model.CharacterDao;
import com.yichi.onepiece.character.model.CharacterEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CharacterHelper {
  private final CharacterDao characterDao;

  public CharacterHelper(CharacterDao characterDao) {
    this.characterDao = characterDao;
  }

  // list all fruit holders of certain fruit
  public List<CharacterEntity> listByFruitId(UUID fruitId) {
    return characterDao.listByFruitId(fruitId);
  }
}
