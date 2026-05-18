
package com.example.saas.security;

import com.example.saas.exception.ForbiddenException;
import com.example.saas.model.Enums.UserRole;
import com.example.saas.model.Enums.UserStatus;
import com.example.saas.model.User;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class LoggedInUser implements UserDetails {

  private final UUID userId;
  private final UUID tenantId;
  private final String email;
  private final UserRole role;
  private final String passwordHash;
  private final boolean active;

  public LoggedInUser(User user) {
    this.userId = user.getId();
    this.tenantId = user.getTenantId();
    this.email = user.getEmail();
    this.role = user.getRole();
    this.passwordHash = user.getPasswordHash();
    this.active = user.getStatus() == UserStatus.active;
  }

  public static LoggedInUser current() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof LoggedInUser user)) {
      throw new ForbiddenException("Not logged in");
    }
    return user;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getTenantId() {
    return tenantId;
  }

  public UserRole getRole() {
    return role;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name().toUpperCase()));
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isEnabled() {
    return active;
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
}
