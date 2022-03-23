package com.yichi.onepiece.Login.Model;

@javax.annotation.processing.Generated("com.yichi.infra.support.entity.EntityProcessor")
public class UserEntityBuilderImpl implements UserEntityBuilder {

  private java.util.UUID id;
  private java.lang.String userName;
  private java.lang.String organization;

  public UserEntityBuilderImpl() {
  }

  public UserEntityBuilderImpl(java.util.UUID id, java.lang.String userName, java.lang.String organization) {
    setId(id);
    setUserName(userName);
    setOrganization(organization);
  }

  public UserEntityBuilderImpl(UserEntity entity) {
    setId(entity.getId());
    setUserName(entity.getUserName());
    setOrganization(entity.getOrganization());
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
  public UserEntityBuilder clone() {
    return UserEntityBuilder.from(this.build());
  }

  @Override
  public UserEntity build() {
    return new UserEntityImpl(getId(), getUserName(), getOrganization());
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
    if (o instanceof UserEntityBuilder) {
      UserEntityBuilder that = (UserEntityBuilder) o;
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
