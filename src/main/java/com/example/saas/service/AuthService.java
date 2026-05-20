package com.example.saas.service;

import com.example.saas.dto.Dtos.AuthResponse;
import com.example.saas.model.Tenant;
import com.example.saas.model.User;
import com.example.saas.repository.TenantRepository;
import com.example.saas.repository.UserRepository;
import com.example.saas.security.JwtUtil;
import com.example.saas.util.TenantCtx;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  public AuthService(
      TenantRepository tenantRepository,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtUtil jwtUtil) {
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
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
    admin = userRepository.save(admin);

    String access = jwtUtil.createAccessToken(admin.getId(), tenant.getId(), admin.getEmail(), admin.getRole());
    String refresh = UUID.randomUUID().toString(); // Stateless tracking fallback for this commit

    return new AuthResponse(
        access,
        refresh,
        UUID.randomUUID(),
        jwtUtil.accessTokenSeconds());
  }

  @Transactional
  public AuthResponse login(String slug, String email, String password, String ip, String userAgent) {
    Tenant tenant = tenantRepository.findBySlug(slug)
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    TenantCtx.set(tenant.getId());

    // Lookup user inside the boundary
    User user = userRepository.findByEmailAndTenantId(email, tenant.getId())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid credentials");
    }

    String access = jwtUtil.createAccessToken(user.getId(), tenant.getId(), user.getEmail(), user.getRole());
    String refresh = UUID.randomUUID().toString();

    return new AuthResponse(
        access,
        refresh,
        UUID.randomUUID(),
        jwtUtil.accessTokenSeconds());
  }
}
