package com.yichi.onepiece.Character.Controller;

import com.yichi.onepiece.Character.Service.CharacterService;
import com.yichi.onepiece.api.CharacterApi;
import com.yichi.onepiece.model.AddCharacterRequest;
import com.yichi.onepiece.model.AddProfileResponse;
import com.yichi.onepiece.model.Character;
import com.yichi.onepiece.model.EatFruitRequest;
import com.yichi.onepiece.model.EatFruitResponse;
import com.yichi.onepiece.model.ListCharacterRequest;
import com.yichi.onepiece.model.ListCharacterResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

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
  public ResponseEntity<AddProfileResponse> addProfile(
      UUID characterId, MultipartFile profileImage) {
    var character = characterService.addProfile(characterId, profileImage);
    return ResponseEntity.ok(new AddProfileResponse().character(character));
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
