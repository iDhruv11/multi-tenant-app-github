package com.example.saas.dto;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UUID sessionId,
    long accessTokenExpiresInSeconds) {
}
