package com.yichi.onepiece.battle.model;

public class CurrentCharacter {
  private Integer blood;
  private Integer magic;
  private Integer energyPercentage;
  private Integer attack;
  private Integer speed;
  private Integer defense;

  public CurrentCharacter() {}

  public CurrentCharacter(Integer blood, Integer magic, Integer energyPercentage,
      Integer attack, Integer speed, Integer defense) {
    this.attack = attack;
    this.speed = speed;
    this.energyPercentage = energyPercentage;
    this.blood = blood;
    this.magic = magic;
    this.defense = defense;
  }

  public Integer getBlood() {
    return blood;
  }

  public void setBlood(Integer blood) {
    this.blood = blood;
  }

  public Integer getMagic() {
    return magic;
  }

  public void setMagic(Integer magic) {
    this.magic = magic;
  }

  public Integer getEnergyPercentage() {
    return energyPercentage;
  }

  public void setEnergyPercentage(Integer energyPercentage) {
    this.energyPercentage = energyPercentage;
  }

  public Integer getAttack() {
    return attack;
  }

  public void setAttack(Integer attack) {
    this.attack = attack;
  }

  public Integer getSpeed() {
    return speed;
  }

  public void setSpeed(Integer speed) {
    this.speed = speed;
  }

  public Integer getDefense() {
    return defense;
  }

  public void setDefense(Integer defense) {
    this.defense = defense;
  }
}
