package com.example.saas.service;

import com.example.saas.dto.Dtos.AuthResponse;
import com.example.saas.model.Tenant;
import com.example.saas.model.User;
import com.example.saas.repository.TenantRepository;
import com.example.saas.repository.UserRepository;
import com.example.saas.util.TenantCtx;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AuthService {

  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      TenantRepository tenantRepository,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder) {
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public AuthResponse register(
      String tenantName, String slug, String email, String password, String firstName, String lastName) {

    if (tenantRepository.existsBySlug(slug)) {
      throw new RuntimeException("Slug already taken");
    }
    Tenant tenant = new Tenant();
    tenant.setName(tenantName);
    tenant.setSlug(slug);
    tenant = tenantRepository.save(tenant);

    TenantCtx.set(tenant.getId());

    User admin = new User();
    admin.setTenantId(tenant.getId());
    admin.setEmail(email);
    admin.setPasswordHash(passwordEncoder.encode(password));
    admin.setFirstName(firstName);
    admin.setLastName(lastName);
    userRepository.save(admin);

    return new AuthResponse(
        "stub-access-token",
        "stub-refresh-token",
        UUID.randomUUID(),
        3600);
  }
}
