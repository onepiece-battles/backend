package com.yichi.onepiece.Character.Controller;

import com.yichi.onepiece.Character.Service.CharacterService;
import com.yichi.onepiece.api.CharacterApi;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.model.Character;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class CharacterController implements CharacterApi {

  private final CharacterService characterService;

  public CharacterController(CharacterService characterService) {
    this.characterService = characterService;
  }

  @Override
  public ResponseEntity<Character> addCharacter(AddCharacterRequest addCharacterRequest) {
    var characterEntity = characterService.addCharacter(addCharacterRequest);
    System.out.println(characterEntity);
    return ResponseEntity.ok(characterEntity);
  }

  @Override
  public ResponseEntity<EatFruitResponse> eatFruit(EatFruitRequest eatFruitRequest) {
    return ResponseEntity.ok(
        characterService.eatFruit(eatFruitRequest.getCharacterId(), eatFruitRequest.getFruitId()));
  }

  @Override
  public ResponseEntity<ListCharacterResponse> listCharacters(
      ListCharacterRequest listCharacterRequest) {
    ListCharacterResponse response = new ListCharacterResponse();
    response.setCharacters(characterService.listCharacters(listCharacterRequest));
    return ResponseEntity.ok(response);
  }
}
