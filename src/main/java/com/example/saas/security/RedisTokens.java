
package com.example.saas.security;

import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisTokens {

  private final StringRedisTemplate redis;

  public RedisTokens(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public void save(String refreshToken, UUID sessionId, Duration ttl) {
    redis.opsForValue().set("refresh:" + refreshToken, sessionId.toString(), ttl);
  }

  public UUID lookup(String refreshToken) {
    String id = redis.opsForValue().get("refresh:" + refreshToken);
    return id == null ? null : UUID.fromString(id);
  }

  public void delete(String refreshToken) {
    redis.delete("refresh:" + refreshToken);
  }
}
