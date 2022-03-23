package com.yichi.onepiece.Character.Model;

@javax.annotation.processing.Generated("com.yichi.infra.support.entity.EntityProcessor")
public class CasebookEntityBuilderImpl implements CasebookEntityBuilder {

  private java.util.UUID casebookId;
  private java.lang.Long casebookNumber;

  public CasebookEntityBuilderImpl() {
  }

  public CasebookEntityBuilderImpl(java.util.UUID casebookId, java.lang.Long casebookNumber) {
    setCasebookId(casebookId);
    setCasebookNumber(casebookNumber);
  }

  public CasebookEntityBuilderImpl(CasebookEntity entity) {
    setCasebookId(entity.getCasebookId());
    setCasebookNumber(entity.getCasebookNumber());
  }

  @Override
  public java.util.UUID getCasebookId() {
    return casebookId;
  }

  @Override
  public CasebookEntityBuilder setCasebookId(java.util.UUID casebookId) {
    this.casebookId = casebookId;
    return this;
  }

  @Override
  public CasebookEntityBuilder casebookId(java.util.UUID casebookId) {
    return setCasebookId(casebookId);
  }

  @Override
  public java.lang.Long getCasebookNumber() {
    return casebookNumber;
  }

  @Override
  public CasebookEntityBuilder setCasebookNumber(java.lang.Long casebookNumber) {
    this.casebookNumber = casebookNumber;
    return this;
  }

  @Override
  public CasebookEntityBuilder casebookNumber(java.lang.Long casebookNumber) {
    return setCasebookNumber(casebookNumber);
  }

  @Override
  public CasebookEntityBuilder clone() {
    return CasebookEntityBuilder.from(this.build());
  }

  @Override
  public CasebookEntity build() {
    return new CasebookEntityImpl(getCasebookId(), getCasebookNumber());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getCasebookId(), getCasebookNumber());
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (o == null) {
      return false;
    }
    if (this == o) {
      return true;
    }
    if (o instanceof CasebookEntityBuilder) {
      CasebookEntityBuilder that = (CasebookEntityBuilder) o;
      return java.util.Objects.equals(getCasebookId(), that.getCasebookId()) &&
          java.util.Objects.equals(getCasebookNumber(), that.getCasebookNumber());
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ").append(getClass().getName()).append(" {\n");
    sb.append("  casebookId: ").append(toIndentedString(getCasebookId())).append("\n");
    sb.append("  casebookNumber: ").append(toIndentedString(getCasebookNumber())).append("\n");
    sb.append("}");
    return sb.toString();
  }

  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n  ");
  }
}
