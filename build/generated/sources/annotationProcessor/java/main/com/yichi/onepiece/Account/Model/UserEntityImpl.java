package com.yichi.onepiece.Account.Model;

@javax.annotation.processing.Generated("com.yichi.infra.support.entity.EntityProcessor")
public class UserEntityImpl implements UserEntity, com.yichi.infra.support.entity.EntityImpl {

  private java.util.UUID id;
  private java.lang.String userName;
  private java.lang.String organization;
  private java.lang.String encryptedPassword;
  private com.yichi.onepiece.Account.Model.AccountCondition accountCondition;

  public UserEntityImpl() {}

  public UserEntityImpl(
      java.util.UUID id,
      java.lang.String userName,
      java.lang.String organization,
      java.lang.String encryptedPassword,
      com.yichi.onepiece.Account.Model.AccountCondition accountCondition) {
    setId(id);
    setUserName(userName);
    setOrganization(organization);
    setEncryptedPassword(encryptedPassword);
    setAccountCondition(accountCondition);
  }

  public UserEntityImpl(UserEntity entity) {
    setId(entity.getId());
    setUserName(entity.getUserName());
    setOrganization(entity.getOrganization());
    setEncryptedPassword(entity.getEncryptedPassword());
    setAccountCondition(entity.getAccountCondition());
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
  public java.lang.String getEncryptedPassword() {
    return encryptedPassword;
  }

  public UserEntityImpl setEncryptedPassword(java.lang.String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
    return this;
  }

  @Override
  public com.yichi.onepiece.Account.Model.AccountCondition getAccountCondition() {
    return accountCondition;
  }

  public UserEntityImpl setAccountCondition(
      com.yichi.onepiece.Account.Model.AccountCondition accountCondition) {
    this.accountCondition = accountCondition;
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
    if ("encryptedPassword".equals(name)) {
      return getEncryptedPassword();
    }
    if ("accountCondition".equals(name)) {
      return getAccountCondition();
    }
    throw new NoSuchFieldException("no such field:" + name);
  }

  @Override
  @java.lang.SuppressWarnings("unchecked")
  public void set(String name, Object obj) throws NoSuchFieldException {
    if ("id".equals(name)) {
      setId((java.util.UUID) obj);
      return;
    }
    if ("userName".equals(name)) {
      setUserName((java.lang.String) obj);
      return;
    }
    if ("organization".equals(name)) {
      setOrganization((java.lang.String) obj);
      return;
    }
    if ("encryptedPassword".equals(name)) {
      setEncryptedPassword((java.lang.String) obj);
      return;
    }
    if ("accountCondition".equals(name)) {
      setAccountCondition((com.yichi.onepiece.Account.Model.AccountCondition) obj);
      return;
    }
    throw new NoSuchFieldException("no such field:" + name);
  }

  @Override
  public UserEntityBuilder toBuilder() {
    return UserEntityBuilder.from(this);
  }

  @Override
  public UserEntity clone() {
    return UserEntityBuilder.from(this).build();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        getId(), getUserName(), getOrganization(), getEncryptedPassword(), getAccountCondition());
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
      return java.util.Objects.equals(getId(), that.getId())
          && java.util.Objects.equals(getUserName(), that.getUserName())
          && java.util.Objects.equals(getOrganization(), that.getOrganization())
          && java.util.Objects.equals(getEncryptedPassword(), that.getEncryptedPassword())
          && java.util.Objects.equals(getAccountCondition(), that.getAccountCondition());
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
    sb.append("  encryptedPassword: ")
        .append(toIndentedString(getEncryptedPassword()))
        .append("\n");
    sb.append("  accountCondition: ").append(toIndentedString(getAccountCondition())).append("\n");
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
