package com.yichi.onepiece.user.model;

import com.yichi.onepiece.iam.model.TokenEntity;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class UserDetailImpl extends UserEntityImpl implements UserDetails {
  private Set<String> userRoles;
  private List<TokenEntity> tokens;

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

  public Set<String> getUserRoles() {
    return userRoles;
  }

  public void setUserRoles(Set<String> userRoles) {
    this.userRoles = userRoles;
  }

  @Override
  public String getPassword() {
    return getEncryptedPassword();
  }

  @Override
  public String getUsername() {
    return getName();
  }

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
    sb.append("  name: ").append(toIndentedString(getName())).append("\n");
    sb.append("  email: ").append(toIndentedString(getEmail())).append("\n");
    sb.append("  organization: ").append(toIndentedString(getOrganization())).append("\n");
    sb.append("  encryptedPassword: ")
        .append(toIndentedString(getEncryptedPassword()))
        .append("\n");
    sb.append("  accountCondition: ").append(toIndentedString(getAccountCondition())).append("\n");
    sb.append("  profileImage: ").append(toIndentedString(getProfileImage())).append("\n");
    sb.append("  userRoles: ").append(toIndentedString(getUserRoles())).append("\n");
    sb.append("  tokens: ").append(toIndentedString(getTokens())).append("\n");
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
