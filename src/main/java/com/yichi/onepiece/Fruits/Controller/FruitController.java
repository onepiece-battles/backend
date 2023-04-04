package com.yichi.onepiece.Fruits.Controller;

import com.yichi.onepiece.Fruits.Service.FruitService;
import com.yichi.onepiece.api.FruitApi;
import com.yichi.onepiece.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class FruitController implements FruitApi {
    private final FruitService fruitService;

    public FruitController(FruitService fruitService) {
        this.fruitService = fruitService;
    }

    @Override
    public ResponseEntity<Fruit> addFruit(AddFruitRequest addFruitRequest) {
        return ResponseEntity.ok(fruitService.addFruit(addFruitRequest));
    }

    @Override
    public ResponseEntity<ListFruitResponse> listFruit(ListFruitRequest listFruitRequest) {
        ListFruitResponse response = new ListFruitResponse();
        response.setFruits(fruitService.listFruit(listFruitRequest));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Fruit> updateFruit(UpdateFruitRequest updateFruitRequest) {
        return ResponseEntity.ok(fruitService.updateFruit(updateFruitRequest));
    }
}
