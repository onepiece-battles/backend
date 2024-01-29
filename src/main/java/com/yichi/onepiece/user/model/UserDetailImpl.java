package com.yichi.onepiece.user.model;

import com.yichi.onepiece.iam.model.TokenEntity;
import com.yichi.onepiece.lineage.model.ActionRecordEntity;
import com.yichi.onepiece.model.ResourceType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserDetailImpl extends UserEntityImpl
    implements UserEntity, UserDetails, com.yichi.infra.support.entity.EntityImpl {
  // not using enum here is because the transform of GrantedAuthority to User.
  private List<String> userRoles;
  private List<TokenEntity> tokens;

  private List<ResourceType> loginRoleResources;

  private ActionRecordEntity creationRecord;
  private ActionRecordEntity modificationRecord;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return getUserRoles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
  }

  public List<TokenEntity> getTokens() {
    return tokens;
  }

  public void setTokens(List<TokenEntity> tokens) {
    this.tokens = tokens;
  }

  public List<String> getUserRoles() {
    return userRoles;
  }

  public UserEntityImpl setUserRoles(List<String> userRoles) {
    this.userRoles = userRoles;
    return this;
  }

  public UserEntityImpl setCreationRecord(ActionRecordEntity creationRecord) {
    this.creationRecord = creationRecord;
    return this;
  }

  public UserEntityImpl setModificationRecord(ActionRecordEntity modificationRecord) {
    this.modificationRecord = modificationRecord;
    return this;
  }

  public UserEntityImpl setLoginRoleResources(List<ResourceType> loginRoleResources) {
    this.loginRoleResources = loginRoleResources;
    return this;
  }

  public ActionRecordEntity getCreationRecord() {
    return this.creationRecord;
  }

  public ActionRecordEntity getModificationRecord() {
    return this.modificationRecord;
  }

  public List<ResourceType> getLoginRoleResources() {
    return loginRoleResources;
  }

  @Override
  public String getPassword() {
    return getEncryptedPassword();
  }

  //  @Override
  //  public String getUsername() {
  //    return getUsername();
  //  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ").append(getClass().getName()).append(" {\n");
    sb.append("  id: ").append(toIndentedString(getId())).append("\n");
    sb.append("  nickname: ").append(toIndentedString(getNickname())).append("\n");
    sb.append("  username: ").append(toIndentedString(getUsername())).append("\n");
    sb.append("  encryptedPassword: ")
        .append(toIndentedString(getEncryptedPassword()))
        .append("\n");
    sb.append("  accountStatus: ").append(toIndentedString(getAccountStatus())).append("\n");
    sb.append("  emailVerified: ").append(toIndentedString(getEmailVerified())).append("\n");
    sb.append("  locale: ").append(toIndentedString(getLocale())).append("\n");
    sb.append("  creationRecordId: ").append(toIndentedString(getCreationRecordId())).append("\n");
    sb.append("  modificationRecordId: ")
        .append(toIndentedString(getModificationRecordId()))
        .append("\n");
    sb.append("  loginRole: ").append(toIndentedString(getLoginRole())).append("\n");
    sb.append("  profileImage: ").append(toIndentedString(getProfileImage())).append("\n");
    sb.append("  userRoles: ").append(toIndentedString(getUserRoles())).append("\n");
    sb.append("  tokens: ").append(toIndentedString(getTokens())).append("\n");
    sb.append("  creationRecord: ").append(toIndentedString(getCreationRecord())).append("\n");
    sb.append("  modificationRecord: ")
        .append(toIndentedString(getModificationRecord()))
        .append("\n");
    sb.append("  loginRoleResources: ")
        .append(toIndentedString(getLoginRoleResources()))
        .append("\n");
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
