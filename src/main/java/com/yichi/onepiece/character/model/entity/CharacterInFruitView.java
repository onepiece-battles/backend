package com.yichi.onepiece.character.model.entity;

import com.yichi.infra.support.entity.Entity;
import com.yichi.infra.support.mybatis.annotations.Column;
import com.yichi.onepiece.model.I18nText;

import javax.annotation.Nullable;
import java.math.BigInteger;

@Entity
public interface CharacterInFruitView extends CharacterToFruitEntity, CharacterEntity {
}
