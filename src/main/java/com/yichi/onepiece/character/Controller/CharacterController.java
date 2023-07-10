package com.yichi.onepiece.character.Controller;

import com.yichi.onepiece.api.CharacterApi;
import com.yichi.onepiece.character.Model.CharacterEntity;
import com.yichi.onepiece.character.Service.CharacterService;
import com.yichi.onepiece.iam.IamService;
import com.yichi.onepiece.iam.PermissionDefinition.PermissionEnum;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.model.Character;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class CharacterController implements CharacterApi {

  private final CharacterService characterService;
  private final IamService iamService;

  public CharacterController(CharacterService characterService, IamService iamService) {
    this.characterService = characterService;
    this.iamService = iamService;
  }

  @Override
  public ResponseEntity<Character> addCharacter(AddCharacterRequest addCharacterRequest) {
    iamService.checkPermissionOfCurrentAccount(
        CharacterEntity.ENTITY_TYPE,
        List.of(PermissionEnum.CHARACTER_CREATE, PermissionEnum.CHARACTER_GET));
    var characterEntity = characterService.addCharacter(addCharacterRequest);
    System.out.println(characterEntity);
    return ResponseEntity.ok(characterEntity);
  }

  @Override
  public ResponseEntity<EatFruitResponse> eatFruit(EatFruitRequest eatFruitRequest) {
    iamService.checkPermissionOfCurrentAccount(
        CharacterEntity.ENTITY_TYPE,
        List.of(PermissionEnum.CHARACTER_UPDATE, PermissionEnum.CHARACTER_GET));
    return ResponseEntity.ok(
        characterService.eatFruit(eatFruitRequest.getCharacterId(), eatFruitRequest.getFruitId()));
  }

  @Override
  public ResponseEntity<ListCharacterResponse> listCharacters(
      ListCharacterRequest listCharacterRequest) {
    iamService.checkPermissionOfCurrentAccount(
        CharacterEntity.ENTITY_TYPE, List.of(PermissionEnum.CHARACTER_LIST));
    ListCharacterResponse response = new ListCharacterResponse();
    response.setCharacters(characterService.listCharacters(listCharacterRequest));
    return ResponseEntity.ok(response);
  }
}
