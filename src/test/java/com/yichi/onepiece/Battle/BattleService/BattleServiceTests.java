package com.yichi.onepiece.Battle.BattleService;

import static org.mockito.Mockito.when;

import com.yichi.onepiece.Battle.Model.BattleDao;
import com.yichi.onepiece.Battle.Model.BattleEntity;
import com.yichi.onepiece.Battle.Model.BattleEntityBuilder;
import com.yichi.onepiece.Battle.Service.BattleService;
import com.yichi.onepiece.Character.Model.CharacterEntity;
import com.yichi.onepiece.Character.Model.CharacterEntityBuilder;
import com.yichi.onepiece.Character.Service.CharacterService;
import com.yichi.onepiece.Fruits.Model.FruitEntity;
import com.yichi.onepiece.Fruits.Model.FruitEntityBuilder;
import com.yichi.onepiece.Fruits.Service.FruitService;
import com.yichi.onepiece.model.FruitInCharacter;
import com.yichi.onepiece.model.FruitType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(MockitoTestNGListener.class)
public class BattleServiceTests {
  @Mock BattleDao mockBattleDao;
  @Mock FruitService mockFruitService;
  @Mock CharacterService mockCharacterService;

  BattleService battleService;

  @BeforeMethod
  public void setUp() {
    battleService = new BattleService(mockBattleDao, mockCharacterService, mockFruitService);
  }

  @Test
  public void testStartBattle() {
    UUID battleId = UUID.randomUUID();
    UUID characterInitId = UUID.randomUUID();
    UUID characterReceiveId = UUID.randomUUID();
    // nika fruit
    UUID fruit1Id = UUID.randomUUID();
    // dart fruit
    UUID fruit2Id = UUID.randomUUID();
    // earthquake fruit
    UUID fruit3Id = UUID.randomUUID();
    BattleEntity battleEntity =
        BattleEntityBuilder.newBuilder()
            .setId(battleId)
            .setCharacterIdInitiator(characterInitId)
            .setCharacterIdReceiver(characterReceiveId)
            .build();
    CharacterEntity characterInit =
        CharacterEntityBuilder.newBuilder()
            .setId(characterInitId)
            .setFruitLimit(1)
            .setName("Luffy")
            .setAttack(950)
            .setDefense(750)
            .setBlood(11000)
            .setMagic(100)
            .fruits(List.of(new FruitInCharacter().fruitId(fruit1Id)))
            .setHasFruits(true)
            .setSpeed(160)
            .setIntelligence(220)
            .build();

    CharacterEntity characterReceive =
        CharacterEntityBuilder.newBuilder()
            .setId(characterReceiveId)
            .setFruitLimit(1)
            .setName("Teach")
            .setAttack(700)
            .setDefense(800)
            .setBlood(8000)
            .setMagic(10000)
            .fruits(
                List.of(
                    new FruitInCharacter().fruitId(fruit2Id),
                    new FruitInCharacter().fruitId(fruit3Id)))
            .setHasFruits(true)
            .setSpeed(160)
            .setIntelligence(220)
            .build();

    FruitEntity fruit1 =
        FruitEntityBuilder.newBuilder()
            .setId(fruit1Id)
            .setName("Nika")
            .setAttackStrengthen(1.2)
            .setDefenseStrengthen(1.5)
            .setIntelligenceStrengthen(1.0)
            .setBloodStrengthen(1.6)
            .setMagicConsume(1)
            .setSpeedStrengthen(1.2)
            .setFruitType(FruitType.PARAMECIA)
            .setOwnerId(characterInitId)
            .build();

    FruitEntity fruit2 =
        FruitEntityBuilder.newBuilder()
            .setId(fruit2Id)
            .setName("Black")
            .setAttackStrengthen(1.3)
            .setDefenseStrengthen(1.3)
            .setIntelligenceStrengthen(1.0)
            .setBloodStrengthen(0.8)
            .setMagicConsume(2)
            .setSpeedStrengthen(1.0)
            .setFruitType(FruitType.PARAMECIA)
            .setOwnerId(characterReceiveId)
            .build();

    FruitEntity fruit3 =
        FruitEntityBuilder.newBuilder()
            .setId(fruit3Id)
            .setName("Earthquake")
            .setAttackStrengthen(1.6)
            .setDefenseStrengthen(1.15)
            .setIntelligenceStrengthen(1.0)
            .setBloodStrengthen(1.0)
            .setMagicConsume(3)
            .setSpeedStrengthen(1.0)
            .setFruitType(FruitType.PARAMECIA)
            .setOwnerId(characterReceiveId)
            .build();
    when(mockBattleDao.findById(battleId)).thenReturn(Optional.of(battleEntity));
    when(mockCharacterService.getCharacterEntityById(characterInitId)).thenReturn(characterInit);
    when(mockCharacterService.getCharacterEntityById(characterReceiveId))
        .thenReturn(characterReceive);
    when(mockFruitService.getFruitsByIds(List.of(fruit1Id))).thenReturn(List.of(fruit1));
    when(mockFruitService.getFruitsByIds(List.of(fruit2Id, fruit3Id)))
        .thenReturn(List.of(fruit2, fruit3));
    battleService.startBattle(battleId);
  }
}
