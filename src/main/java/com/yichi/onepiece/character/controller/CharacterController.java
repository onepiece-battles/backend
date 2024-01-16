package com.yichi.onepiece.character.controller;

import com.yichi.onepiece.api.CharacterApi;
import com.yichi.onepiece.character.service.CharacterService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import com.yichi.onepiece.model.Character;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StopWatch;

@Controller
public class CharacterController implements CharacterApi {
  Logger logger = LoggerFactory.getLogger(CharacterController.class);

  private final CharacterService characterService;
  private final IamService iamService;

  public CharacterController(CharacterService characterService, IamService iamService) {
    this.characterService = characterService;
    this.iamService = iamService;
  }

  @Override
  public ResponseEntity<Character> addCharacter(CreateCharacterRequest addCharacterRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.CHARACTER, Set.of(Permission.CREATE, Permission.GET));
    var characterEntity = characterService.addCharacter(addCharacterRequest);
    return ResponseEntity.ok(characterEntity);
  }

  @Override
  public ResponseEntity<EatFruitResponse> eatFruit(EatFruitRequest eatFruitRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.CHARACTER, Set.of(Permission.UPDATE, Permission.GET));
    return ResponseEntity.ok(
        characterService.eatFruit(eatFruitRequest.getCharacterId(), eatFruitRequest.getFruitId()));
  }

  @Override
  public ResponseEntity<CharacterFullView> getCharacterFullView(
      GetCharacterFullViewRequest getCharacterFullViewRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of(Permission.GET));
    iamService.checkPermissionOfCurrentAccount(ResourceType.BATTLE, Set.of(Permission.GET));
    iamService.checkPermissionOfCurrentAccount(ResourceType.FRUIT, Set.of(Permission.GET));
    iamService.checkPermissionOfCurrentAccount(ResourceType.GROUP, Set.of(Permission.GET));
    StopWatch watch = new StopWatch();
    watch.start();
    var characterFullView =
        characterService.getCharacterFullView(getCharacterFullViewRequest.getCharacterId());
    watch.stop();
    logger.info("get character takes: {}", watch.getTotalTimeSeconds());
    return ResponseEntity.ok(characterFullView);
  }

  @Override
  public ResponseEntity<JoinGroupResponse> joinGroup(JoinGroupRequest joinGroupRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of(Permission.UPDATE));
    var response = characterService.joinGroup(joinGroupRequest);
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ListCharacterResponse> listCharacters(
      ListCharacterRequest listCharacterRequest) {
    logger.info("list character api");
    iamService.checkPermissionOfCurrentAccount(ResourceType.CHARACTER, Set.of(Permission.LIST));
    StopWatch watch = new StopWatch();
    watch.start();
    ListCharacterResponse response = characterService.listCharacters(listCharacterRequest);
    watch.stop();
    logger.info("list character takes: {}", watch.getTotalTimeSeconds());
    return ResponseEntity.ok(response);
  }
}
