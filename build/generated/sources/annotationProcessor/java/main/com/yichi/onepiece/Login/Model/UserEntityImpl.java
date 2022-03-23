package com.yichi.onepiece.Login.Model;

@javax.annotation.processing.Generated("com.yichi.infra.support.entity.EntityProcessor")
public class UserEntityImpl implements UserEntity, com.yichi.infra.support.entity.EntityImpl {

  private java.util.UUID id;
  private java.lang.String userName;
  private java.lang.String organization;

  public UserEntityImpl() {
  }

  public UserEntityImpl(java.util.UUID id, java.lang.String userName, java.lang.String organization) {
    setId(id);
    setUserName(userName);
    setOrganization(organization);
  }

  public UserEntityImpl(UserEntity entity) {
    setId(entity.getId());
    setUserName(entity.getUserName());
    setOrganization(entity.getOrganization());
  }

  @Override
  public java.util.UUID getId() {
    return id;
  }

  public UserEntityImpl setId(java.util.UUID id) {
    this.id = id;
    return this;
  }

  @Override
  public java.lang.String getUserName() {
    return userName;
  }

  public UserEntityImpl setUserName(java.lang.String userName) {
    this.userName = userName;
    return this;
  }

  @Override
  public java.lang.String getOrganization() {
    return organization;
  }

  public UserEntityImpl setOrganization(java.lang.String organization) {
    this.organization = organization;
    return this;
  }

  @Override
  @java.lang.SuppressWarnings("unchecked")
  public Object get(String name) throws NoSuchFieldException {
    if ("id".equals(name)) {
      return getId();
    }
    if ("userName".equals(name)) {
      return getUserName();
    }
    if ("organization".equals(name)) {
      return getOrganization();
    }
    throw new NoSuchFieldException("no such field:" + name);
  }

  @Override
  @java.lang.SuppressWarnings("unchecked")
  public void set(String name, Object obj) throws NoSuchFieldException {
    if ("id".equals(name)) {
      setId((java.util.UUID)obj);
      return;
    }
    if ("userName".equals(name)) {
      setUserName((java.lang.String)obj);
      return;
    }
    if ("organization".equals(name)) {
      setOrganization((java.lang.String)obj);
      return;
    }
    throw new NoSuchFieldException("no such field:" + name);
  }

  @Override
  public UserEntity clone() {
    return UserEntityBuilder.from(this).build();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(getId(), getUserName(), getOrganization());
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (o == null) {
      return false;
    }
    if (this == o) {
      return true;
    }
    if (o instanceof UserEntity) {
      UserEntity that = (UserEntity) o;
      return java.util.Objects.equals(getId(), that.getId()) &&
          java.util.Objects.equals(getUserName(), that.getUserName()) &&
          java.util.Objects.equals(getOrganization(), that.getOrganization());
    } else {
      return false;
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ").append(getClass().getName()).append(" {\n");
    sb.append("  id: ").append(toIndentedString(getId())).append("\n");
    sb.append("  userName: ").append(toIndentedString(getUserName())).append("\n");
    sb.append("  organization: ").append(toIndentedString(getOrganization())).append("\n");
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
