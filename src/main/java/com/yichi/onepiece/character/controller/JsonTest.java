package com.yichi.onepiece.character.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yichi.onepiece.character.model.CharacterListViewBuilder;
import com.yichi.onepiece.character.model.converter.CharacterEntityConverter;
import com.yichi.onepiece.model.CharacterType;
import com.yichi.onepiece.model.ListCharacterResponse;
import java.util.List;

public class JsonTest {
  public static void main(String[] args) {
    var characterEntity =
        CharacterListViewBuilder.newBuilder()
            .setCharacterTypes(List.of(CharacterType.SCIENCE, CharacterType.BODY))
            .setSpeed(100)
            .setMaximumFruitCount(1)
            .setIntelligence(100)
            .setDefense(100)
            .setBlood(1000)
            .setGroupGain(2)
            .setAttack(100)
            .setAge(10)
            .setName("aa")
            .setBirthPlace("aa")
            .setBounty(11)
            .setEnergy(100)
            .setHeight(10.3)
            .build();
    var character = CharacterEntityConverter.INSTANCE.toCharacterListView(characterEntity);
    var mapper = new ObjectMapper();
    var response = new ListCharacterResponse();
    response.characters(List.of(character)).totalSize(1);
    try {
      var json = mapper.writeValueAsString(response);
      System.out.println(json);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }
}
