package com.yichi.onepiece.constants;

public class OnepieceConstants {
  public static final String FROM_EMAIL_ADDRESS = "onepiecebattles@163.com";

  public static final String VALID_EMAIL_PATTERN =
      "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";

  public static final String SUCCESS = "SUCCESS";


  // async constants
  public static final String BATTLE_ID = "BATTLE_ID";
  public static final String USER_ID = "USER_ID";
  public static final String USER_EMAIL = "USER_EMAIL";
  public static final String USER_NICKNAME = "USER_NICKNAME";
  public static final String ROLE_NAME = "ROLE_NAME";
  public static final String EMAIL_JOB_GROUP = "EMAIL_JOB_GROUP";
  public static final String BATTLE_JOB_GROUP = "BATTLE_JOB_GROUP";
  public static final String SUBJECT = "SUBJECT";
  public static final String BODY = "BODY";
  public static final String RECEIVER_EMAIL_ADDRESS = "RECEIVER_EMAIL_ADDRESS";

  // battle digits boundary
  public static final Integer ATTACK_MIN = 10;
  public static final Integer ATTACK_MAX = 1500;
  public static final Integer DEFENSE_MIN = 10;
  public static final Integer DEFENSE_MAX = 1400;
  public static final Integer BLOOD_MIN = 100;
  public static final Integer BLOOD_MAX = 18000;
  public static final Integer SPEED_MIN = 10;
  public static final Integer SPEED_MAX = 300;
  public static final Integer GROUP_GAIN_MIN = 80;
  public static final Integer GROUP_GAIN_MAX = 300;
  public static final Integer INTELLIGENCE_MIN = 50;
  public static final Integer INTELLIGENCE_MAX = 300;

}
