package com.yichi.onepiece.Account.Model;

@javax.annotation.processing.Generated("com.yichi.infra.support.entity.EntityProcessor")
public class UserEntityBuilderImpl implements UserEntityBuilder {

  private java.util.UUID id;
  private java.lang.String userName;
  private java.lang.String organization;
  private java.lang.String encryptedPassword;
  private com.yichi.onepiece.Account.Model.AccountCondition accountCondition;

  public UserEntityBuilderImpl() {}

  public UserEntityBuilderImpl(
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

  public UserEntityBuilderImpl(UserEntity entity) {
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

  @Override
  public UserEntityBuilder setId(java.util.UUID id) {
    this.id = id;
    return this;
  }

  @Override
  public UserEntityBuilder id(java.util.UUID id) {
    return setId(id);
  }

  @Override
  public java.lang.String getUserName() {
    return userName;
  }

  @Override
  public UserEntityBuilder setUserName(java.lang.String userName) {
    this.userName = userName;
    return this;
  }

  @Override
  public UserEntityBuilder userName(java.lang.String userName) {
    return setUserName(userName);
  }

  @Override
  public java.lang.String getOrganization() {
    return organization;
  }

  @Override
  public UserEntityBuilder setOrganization(java.lang.String organization) {
    this.organization = organization;
    return this;
  }

  @Override
  public UserEntityBuilder organization(java.lang.String organization) {
    return setOrganization(organization);
  }

  @Override
  public java.lang.String getEncryptedPassword() {
    return encryptedPassword;
  }

  @Override
  public UserEntityBuilder setEncryptedPassword(java.lang.String encryptedPassword) {
    this.encryptedPassword = encryptedPassword;
    return this;
  }

  @Override
  public UserEntityBuilder encryptedPassword(java.lang.String encryptedPassword) {
    return setEncryptedPassword(encryptedPassword);
  }

  @Override
  public com.yichi.onepiece.Account.Model.AccountCondition getAccountCondition() {
    return accountCondition;
  }

  @Override
  public UserEntityBuilder setAccountCondition(
      com.yichi.onepiece.Account.Model.AccountCondition accountCondition) {
    this.accountCondition = accountCondition;
    return this;
  }

  @Override
  public UserEntityBuilder accountCondition(
      com.yichi.onepiece.Account.Model.AccountCondition accountCondition) {
    return setAccountCondition(accountCondition);
  }

  @Override
  public UserEntityBuilder clone() {
    return UserEntityBuilder.from(this.build());
  }

  @Override
  public UserEntity build() {
    return new UserEntityImpl(
        getId(), getUserName(), getOrganization(), getEncryptedPassword(), getAccountCondition());
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
    if (o instanceof UserEntityBuilder) {
      UserEntityBuilder that = (UserEntityBuilder) o;
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
