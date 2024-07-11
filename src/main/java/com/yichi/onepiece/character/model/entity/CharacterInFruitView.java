package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.mybatis.annotations.Column;
import com.yichi.onepiece.model.I18nText;

import javax.annotation.Nullable;
import java.math.BigInteger;

@Entity
public interface CharacterInFruitView extends CharacterToFruitEntity {
    @Column(json = true)
    I18nText getName();
    String getImageUri();
    Integer getMaximumFruitCount();
    BigInteger getBounty();

    // battle info
    Integer getGroupGain();

    Integer getAttack();

    Integer getDefense();

    Integer getIntelligence();

    Integer getBlood();

    Integer getSpeed();

    Integer getEnergy();
}
