package com.yichi.onepiece.Account.Model;

@javax.annotation.processing.Generated("com.yichi.infra.support.entity.EntityProcessor")
public interface UserEntityBuilder {

  public static UserEntityBuilder newBuilder() {
    return new UserEntityBuilderImpl();
  }

  public static UserEntityBuilder from(UserEntity entity) {
    return new UserEntityBuilderImpl()
        .setId(entity.getId())
        .setUserName(entity.getUserName())
        .setOrganization(entity.getOrganization())
        .setEncryptedPassword(entity.getEncryptedPassword())
        .setAccountCondition(entity.getAccountCondition());
  }

  public static UserEntity fromJSON(String json) {
    return com.yichi.infra.support.entity.EntitySerializer.get()
        .deserialize(json, UserEntity.class);
  }

  public static String toJSON(UserEntity entity) {
    return com.yichi.infra.support.entity.EntitySerializer.get().serialize(entity);
  }

  java.util.UUID getId();

  UserEntityBuilder setId(java.util.UUID id);

  UserEntityBuilder id(java.util.UUID id);

  java.lang.String getUserName();

  UserEntityBuilder setUserName(java.lang.String userName);

  UserEntityBuilder userName(java.lang.String userName);

  java.lang.String getOrganization();

  UserEntityBuilder setOrganization(java.lang.String organization);

  UserEntityBuilder organization(java.lang.String organization);

  java.lang.String getEncryptedPassword();

  UserEntityBuilder setEncryptedPassword(java.lang.String encryptedPassword);

  UserEntityBuilder encryptedPassword(java.lang.String encryptedPassword);

  com.yichi.onepiece.Account.Model.AccountCondition getAccountCondition();

  UserEntityBuilder setAccountCondition(
      com.yichi.onepiece.Account.Model.AccountCondition accountCondition);

  UserEntityBuilder accountCondition(
      com.yichi.onepiece.Account.Model.AccountCondition accountCondition);

  UserEntity build();
}
