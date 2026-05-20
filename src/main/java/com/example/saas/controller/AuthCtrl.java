
package com.example.saas.controller;

import com.example.saas.dto.Dtos.*;
import com.example.saas.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthCtrl {

  private final AuthService authService;

  public AuthCtrl(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/api/v1/auth/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse register(@Valid @RequestBody RegisterRequest body) {
    return authService.register(
        body.tenantName(), body.tenantSlug(), body.email(), body.password(), body.firstName(), body.lastName());
  }

  @PostMapping("/api/v1/auth/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
    return authService.login(
        body.tenantSlug(),
        body.email(),
        body.password(),
        request.getRemoteAddr(),
        request.getHeader("User-Agent"));
  }

  @PostMapping("/api/v1/auth/refresh")
  public AuthResponse refresh(@Valid @RequestBody RefreshRequest body) {
    return authService.refresh(body.refreshToken());
  }

  @PostMapping("/api/v1/auth/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody LogoutRequest body) {
    authService.logout(body.refreshToken());
  }

  @GetMapping("/api/v1/tenant")
  public TenantResponse tenant() {
    return authService.currentTenant();
  }

  @DeleteMapping("/api/v1/tenant")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTenant() {
    authService.deleteCurrentTenant();
  }

  @GetMapping("/api/v1/sessions")
  public List<SessionResponse> sessions(@RequestHeader(value = "X-Session-Id", required = false) UUID sessionId) {
    return authService.listSessions(sessionId);
  }

  @DeleteMapping("/api/v1/sessions/{sessionId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revokeSession(@PathVariable UUID sessionId) {
    authService.revokeSession(sessionId);
  }

  @PostMapping("/api/v1/sessions/revoke-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revokeAll(@RequestHeader(value = "X-Session-Id", required = false) UUID sessionId) {
    authService.revokeAllSessions(sessionId);
  }
}
