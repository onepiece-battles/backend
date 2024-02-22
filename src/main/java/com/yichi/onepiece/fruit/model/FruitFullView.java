package com.yichi.onepiece.fruit.model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.character.model.entity.CharacterInFruitView;
import java.util.List;

@Entity
public interface FruitFullView extends FruitListView{
    List<CharacterInFruitView> getCharacters();
}
