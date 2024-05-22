package com.yichi.onepiece.fruit.controller;

import com.yichi.onepiece.api.FruitApi;
import com.yichi.onepiece.fruit.service.FruitService;
import com.yichi.onepiece.iam.service.IamService;
import com.yichi.onepiece.model.*;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class FruitController implements FruitApi {
  private final FruitService fruitService;
  private final IamService iamService;

  public FruitController(FruitService fruitService, IamService iamService) {
    this.fruitService = fruitService;
    this.iamService = iamService;
  }

  @Override
  public ResponseEntity<Fruit> addFruit(CreateFruitRequest addFruitRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.FRUIT, Set.of(Permission.CREATE, Permission.GET));
    return ResponseEntity.ok(fruitService.createFruit(addFruitRequest));
  }

  @Override
  public ResponseEntity<FruitFullView> getFruit(GetFruitRequest getFruitRequest) {
    iamService.checkPermissionOfCurrentAccount(
            ResourceType.FRUIT, Set.of(Permission.GET));
    iamService.checkPermissionOfCurrentAccount(
            ResourceType.CHARACTER, Set.of(Permission.GET));
    return ResponseEntity.ok(fruitService.getFruit(getFruitRequest));
  }

  @Override
  public ResponseEntity<ListFruitResponse> listFruit(ListFruitRequest listFruitRequest) {
    iamService.checkPermissionOfCurrentAccount(ResourceType.FRUIT, Set.of(Permission.LIST));
    ListFruitResponse response = (fruitService.listFruit(listFruitRequest));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<UpdateFruitResponse> updateFruit(UpdateFruitRequest updateFruitRequest) {
    iamService.checkPermissionOfCurrentAccount(
        ResourceType.FRUIT, Set.of(Permission.UPDATE, Permission.GET));
    return ResponseEntity.ok(fruitService.updateFruit(updateFruitRequest));
  }
}
