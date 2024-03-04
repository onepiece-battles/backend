package com.yichi.onepiece.fruit.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.entity.CharacterInFruitView;
import java.util.List;
import java.util.UUID;

@Entity
public interface FruitFullView extends FruitListView{
    FruitEntity getPreviousFruit();
    FruitEntity getNextFruit();
    List<CharacterInFruitView> getCharacters();

    FruitFullViewBuilder toBuilder();
}
