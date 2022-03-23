package com.yichi.onepiece.Character.Model;

@javax.annotation.processing.Generated("com.yichi.infra.support.entity.EntityProcessor")
public class CasebookEntityImpl implements CasebookEntity, com.yichi.infra.support.entity.EntityImpl {

  private java.util.UUID casebookId;
  private java.lang.Long casebookNumber;

  public CasebookEntityImpl() {
  }

  public CasebookEntityImpl(java.util.UUID casebookId, java.lang.Long casebookNumber) {
    setCasebookId(casebookId);
    setCasebookNumber(casebookNumber);
  }

  public CasebookEntityImpl(CasebookEntity entity) {
    setCasebookId(entity.getCasebookId());
    setCasebookNumber(entity.getCasebookNumber());
  }

  @Override
  public java.util.UUID getCasebookId() {
    return casebookId;
  }

  public CasebookEntityImpl setCasebookId(java.util.UUID casebookId) {
    this.casebookId = casebookId;
    return this;
  }

  @Override
  public java.lang.Long getCasebookNumber() {
    return casebookNumber;
  }

  public CasebookEntityImpl setCasebookNumber(java.lang.Long casebookNumber) {
    this.casebookNumber = casebookNumber;
    return this;
  }

  @Override
  @java.lang.SuppressWarnings("unchecked")
  public Object get(String name) throws NoSuchFieldException {
    if ("casebookId".equals(name)) {
      return getCasebookId();
    }
    if ("casebookNumber".equals(name)) {
      return getCasebookNumber();
    }
    throw new NoSuchFieldException("no such field:" + name);
  }

  @Override
  @java.lang.SuppressWarnings("unchecked")
  public void set(String name, Object obj) throws NoSuchFieldException {
    if ("casebookId".equals(name)) {
      setCasebookId((java.util.UUID)obj);
      return;
    }
    if ("casebookNumber".equals(name)) {
      setCasebookNumber((java.lang.Long)obj);
      return;
    }
    throw new NoSuchFieldException("no such field:" + name);
  }

  @Override
  public CasebookEntity clone() {
    return CasebookEntityBuilder.from(this).build();
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
    if (o instanceof CasebookEntity) {
      CasebookEntity that = (CasebookEntity) o;
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
