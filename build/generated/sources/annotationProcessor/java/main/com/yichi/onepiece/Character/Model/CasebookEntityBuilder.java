package com.yichi.onepiece.Character.Model;

@javax.annotation.processing.Generated("com.yichi.infra.support.entity.EntityProcessor")
public interface CasebookEntityBuilder  {

  public static CasebookEntityBuilder newBuilder() {
    return new CasebookEntityBuilderImpl();
  }

  public static CasebookEntityBuilder from(CasebookEntity entity) {
    return new CasebookEntityBuilderImpl()
        .setCasebookId(entity.getCasebookId())
        .setCasebookNumber(entity.getCasebookNumber());
  }

  public static CasebookEntity fromJSON(String json) {
    return com.yichi.infra.support.entity.EntitySerializer.get().deserialize(json, CasebookEntity.class);
  }

  public static String toJSON(CasebookEntity entity) {
    return com.yichi.infra.support.entity.EntitySerializer.get().serialize(entity);
  }


  java.util.UUID getCasebookId();

  CasebookEntityBuilder setCasebookId(java.util.UUID casebookId);

  CasebookEntityBuilder casebookId(java.util.UUID casebookId);

  java.lang.Long getCasebookNumber();

  CasebookEntityBuilder setCasebookNumber(java.lang.Long casebookNumber);

  CasebookEntityBuilder casebookNumber(java.lang.Long casebookNumber);

  CasebookEntity build();
}

