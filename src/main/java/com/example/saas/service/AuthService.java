package com.example.saas.service;

import com.example.saas.dto.Dtos.AuthResponse;
import com.example.saas.dto.Dtos.SessionResponse;
import com.example.saas.dto.Dtos.TenantResponse;
import com.example.saas.exception.BadRequestException;
import com.example.saas.exception.ForbiddenException;
import com.example.saas.exception.NotFoundException;
import com.example.saas.model.Enums.TenantStatus;
import com.example.saas.model.Enums.UserRole;
import com.example.saas.model.Enums.UserStatus;
import com.example.saas.model.Session;
import com.example.saas.model.Tenant;
import com.example.saas.model.User;
import com.example.saas.repository.SessionRepository;
import com.example.saas.repository.TenantRepository;
import com.example.saas.repository.UserRepository;
import com.example.saas.security.JwtUtil;
import com.example.saas.security.LoggedInUser;
import com.example.saas.security.RedisTokens;
import com.example.saas.util.TenantCtx;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final TenantRepository tenantRepository;
  private final UserRepository userRepository;
  private final SessionRepository sessionRepository;
  private final RedisTokens redisTokens;
  private final JwtUtil jwtUtil;
  private final PasswordEncoder passwordEncoder;
  private final int refreshDays;

  public AuthService(
      TenantRepository tenantRepository,
      UserRepository userRepository,
      SessionRepository sessionRepository,
      RedisTokens redisTokens,
      JwtUtil jwtUtil,
      PasswordEncoder passwordEncoder,
      @Value("${app.jwt.refresh-token-expiration-days}") int refreshDays) {
    this.tenantRepository = tenantRepository;
    this.userRepository = userRepository;
    this.sessionRepository = sessionRepository;
    this.redisTokens = redisTokens;
    this.jwtUtil = jwtUtil;
    this.passwordEncoder = passwordEncoder;
    this.refreshDays = refreshDays;
  }

  @Transactional
  public AuthResponse register(
      String tenantName, String slug, String email, String password, String firstName, String lastName) {
    var tenant = createTenant(tenantName, slug, email, password, firstName, lastName);
    User user = userRepository
        .findByEmailAndTenantId(email, tenant.getId())
        .orElseThrow(() -> new NotFoundException("User missing after signup"));
    return issueTokens(user, null, null);
  }

  @Transactional
  public AuthResponse login(String slug, String email, String password, String ip, String userAgent) {
    var tenant = getBySlug(slug);
    TenantCtx.set(tenant.getId());
    User user = userRepository
        .findByEmailAndTenantId(email, tenant.getId())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    if (user.getStatus() != UserStatus.active
        || !passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    return issueTokens(user, ip, userAgent);
  }

  @Transactional
  public AuthResponse refresh(String refreshToken) {
    UUID sessionId = redisTokens.lookup(refreshToken);
    if (sessionId == null) {
      throw new BadCredentialsException("Invalid refresh token");
    }
    Session session = sessionRepository
        .findById(sessionId)
        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    if (session.getExpiresAt().isBefore(Instant.now())) {
      redisTokens.delete(refreshToken);
      throw new BadCredentialsException("Token expired");
    }
    TenantCtx.set(session.getTenantId());
    User user = userRepository
        .findByIdAndTenantId(session.getUserId(), session.getTenantId())
        .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    String access = jwtUtil.createAccessToken(user.getId(), user.getTenantId(), user.getEmail(), user.getRole());
    return new AuthResponse(access, refreshToken, session.getId(), jwtUtil.accessTokenSeconds());
  }

  @Transactional
  public void logout(String refreshToken) {
    redisTokens.delete(refreshToken);
    sessionRepository.findByRefreshToken(refreshToken).ifPresent(s -> {
      TenantCtx.set(s.getTenantId());
      sessionRepository.delete(s);
    });
  }

  @Transactional(readOnly = true)
  public TenantResponse currentTenant() {
    UUID tenantId = TenantCtx.get();
    Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant not found"));
    return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getSlug());
  }

  @Transactional
  public void deleteCurrentTenant() {
    if (LoggedInUser.current().getRole() != UserRole.admin) {
      throw new ForbiddenException("Admin only");
    }
    deleteTenant(TenantCtx.get());
  }

  @Transactional(readOnly = true)
  public List<SessionResponse> listSessions(UUID currentSessionId) {
    UUID userId = LoggedInUser.current().getUserId();
    return sessionRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, TenantCtx.get()).stream()
        .map(s -> toSessionResponse(s, s.getId().equals(currentSessionId)))
        .toList();
  }

  @Transactional
  public void revokeSession(UUID sessionId) {
    Session session = findOwnSession(sessionId);
    redisTokens.delete(session.getRefreshToken());
    sessionRepository.delete(session);
  }

  @Transactional
  public void revokeAllSessions(UUID exceptSessionId) {
    UUID userId = LoggedInUser.current().getUserId();
    for (Session session : sessionRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(userId, TenantCtx.get())) {
      if (exceptSessionId != null && session.getId().equals(exceptSessionId)) {
        continue;
      }
      redisTokens.delete(session.getRefreshToken());
      sessionRepository.delete(session);
    }
  }

  private Tenant createTenant(String name, String slug, String email, String password, String firstName,
      String lastName) {
    if (tenantRepository.existsBySlug(slug)) {
      throw new BadRequestException("Slug already taken");
    }
    Tenant tenant = new Tenant();
    tenant.setName(name);
    tenant.setSlug(slug);
    tenant = tenantRepository.save(tenant);

    TenantCtx.set(tenant.getId());
    User admin = new User();
    admin.setTenantId(tenant.getId());
    admin.setEmail(email);
    admin.setPasswordHash(passwordEncoder.encode(password));
    admin.setFirstName(firstName);
    admin.setLastName(lastName);
    admin.setRole(UserRole.admin);
    userRepository.save(admin);
    return tenant;
  }

  private Tenant getBySlug(String slug) {
    return tenantRepository
        .findBySlug(slug)
        .filter(t -> t.getStatus() == TenantStatus.active && t.getDeletedAt() == null)
        .orElseThrow(() -> new NotFoundException("Tenant not found"));
  }

  private void deleteTenant(UUID tenantId) {
    Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant not found"));
    tenant.setStatus(TenantStatus.deleted);
    tenant.setDeletedAt(Instant.now());
    tenantRepository.save(tenant);
  }

  private AuthResponse issueTokens(User user, String ip, String userAgent) {
    String access = jwtUtil.createAccessToken(user.getId(), user.getTenantId(), user.getEmail(), user.getRole());
    String refresh = UUID.randomUUID().toString();
    Duration ttl = Duration.ofDays(refreshDays);

    Session session = new Session();
    session.setUserId(user.getId());
    session.setTenantId(user.getTenantId());
    session.setRefreshToken(refresh);
    session.setIpAddress(ip);
    session.setUserAgent(userAgent);
    session.setExpiresAt(Instant.now().plus(ttl));
    session = sessionRepository.save(session);

    redisTokens.save(refresh, session.getId(), ttl);
    return new AuthResponse(access, refresh, session.getId(), jwtUtil.accessTokenSeconds());
  }

  private Session findOwnSession(UUID sessionId) {
    Session session = sessionRepository
        .findById(sessionId)
        .orElseThrow(() -> new NotFoundException("Session not found"));
    if (!session.getUserId().equals(LoggedInUser.current().getUserId())) {
      throw new ForbiddenException("Not your session");
    }
    return session;
  }

  private SessionResponse toSessionResponse(Session s, boolean current) {
    String ua = s.getUserAgent() != null ? s.getUserAgent() : "Unknown";
    String device = s.getIpAddress() != null ? s.getIpAddress() : "Device";
    String browser = ua.length() > 40 ? ua.substring(0, 40) : ua;
    return new SessionResponse(s.getId(), device, browser, s.getCreatedAt(), current);
  }
}
