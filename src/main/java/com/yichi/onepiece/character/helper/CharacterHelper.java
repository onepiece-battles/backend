package com.yichi.onepiece.character.helper;

import com.yichi.onepiece.character.model.dao.CharacterDao;
import com.yichi.onepiece.character.model.entity.CharacterEntity;
import java.util.List;
import java.util.UUID;
import com.yichi.onepiece.fruit.model.FruitListView;
import org.springframework.stereotype.Component;

@Component
public class CharacterHelper {
  private final CharacterDao characterDao;

  public CharacterHelper(CharacterDao characterDao) {
    this.characterDao = characterDao;
  }

  // list all characters that eat this fruit.
  public List<CharacterEntity> listByFruitId(UUID fruitId) {
    return characterDao.listByFruitId(fruitId);
  }



  public void calculateCharacterBattleIndex(CharacterEntity character, List<FruitListView> fruits) {
    for(var fruit: fruits) {
      character =
              character.toBuilder()
              .setAttack((int) (character.getAttack() * fruit.getAttackSlope() + fruit.getAttackIntercept()))
              .setBlood((int) (character.getBlood() * fruit.getBloodSlope() + fruit.getBloodIntercept()))
              .setSpeed((int) (character.getSpeed() * fruit.getSpeedSlope() + fruit.getSpeedIntercept()))
              .setGroupGain((int) (character.getGroupGain() * fruit.getGroupGainSlope() + fruit.getGroupGainIntercept()))
              .setDefense((int) (character.getDefense() * fruit.getDefenseSlope() + fruit.getDefenseIntercept()))
              .setIntelligence((int) (character.getIntelligence() * fruit.getIntelligenceSlope() + fruit.getIntelligenceIntercept()))
              .build();
    }
    characterDao.update(character);
  }
}
