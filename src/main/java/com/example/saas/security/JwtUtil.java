package com.example.saas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private final SecretKey key;
  private final long accessMinutes = 15;

  public JwtUtil(@Value("${app.jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String createAccessToken(UUID userId, UUID tenantId, String email, String role) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId.toString())
        .claim("tenant_id", tenantId.toString())
        .claim("email", email)
        .claim("role", role)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(accessMinutes * 60)))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public boolean isValid(String token) {
    try {
      parse(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public long accessTokenSeconds() {
    return accessMinutes * 60;
  }
}
