package com.example.saas.config;

import com.example.saas.security.JwtUtil;
import com.example.saas.util.TenantCtx;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;

  public TenantFilter(JwtUtil jwtUtil) {
    this.jwtUtil = jwtUtil;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    String requestId = request.getHeader("X-Request-Id");
    if (requestId == null || requestId.isBlank()) {
      requestId = UUID.randomUUID().toString();
    }
    request.setAttribute("requestId", requestId);
    response.setHeader("X-Request-Id", requestId);

    // Early parsing check to extract tenant context out of the JWT token
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      try {
        if (jwtUtil.isValid(token)) {
          Claims claims = jwtUtil.parse(token);
          String tenantIdStr = claims.get("tenant_id", String.class);
          if (tenantIdStr != null) {
            TenantCtx.set(UUID.fromString(tenantIdStr));
          }
        }
      } catch (Exception e) {
        // nothing lol
      }
    }

    try {
      chain.doFilter(request, response);
    } finally {
      // Absolute safety net to clear thread local data at end of request lifecycle
      TenantCtx.clear();
    }
  }
}
