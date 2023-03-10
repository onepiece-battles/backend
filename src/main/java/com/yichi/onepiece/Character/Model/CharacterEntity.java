package com.yichi.onepiece.Character.Model;

import com.yichi.infra.support.entity.Entity;
import com.yichi.onepiece.model.CharacterType;
import com.yichi.onepiece.model.FruitInCharacter;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Entity
public interface CharacterEntity {
    UUID getId();

    CharacterType getCharacterType();

    @Nullable
    Byte[] getProfileImage();

    String getName();

    String getGroupOid();

    Boolean getHasFruits();

    // default to be 1, unless special characters.
    Integer getFruitLimit();

    List<FruitInCharacter> getFruits();

    // some characters are better when fighting in a group
    Integer getGroupGain();

    Integer getAttack();

    Integer getDefense();

    Integer getIntelligence();

    Integer getBlood();

    Integer getSpeed();

    // magic will be used if character has fruit in Paramecia type.
    Integer getMagic();

    CharacterEntityBuilder toBuilder();
}
