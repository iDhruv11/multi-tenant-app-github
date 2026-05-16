package com.example.saas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class Dtos {

  private Dtos() {
  }

  public record ApiResponse<T>(
      T data,
      Instant timestamp,
      String requestId) {
  }

  public record RegisterRequest(
      @NotBlank @Size(max = 255) String tenantName,
      @NotBlank @Size(max = 50) String tenantSlug,
      @NotBlank @Email String email,
      @NotBlank @Size(min = 8) String password,
      @NotBlank String firstName,
      @NotBlank String lastName) {
  }

  public record AuthResponse(
      String accessToken,
      String refreshToken,
      UUID sessionId,
      long accessTokenExpiresInSeconds) {
  }
}
