package com.example.saas.security;

import com.example.saas.repository.SessionRepository;
import com.example.saas.repository.UserRepository;
import com.example.saas.util.TenantCtx;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;
  private final UserRepository userRepository;
  private final SessionRepository sessionRepository;

  public JwtFilter(JwtUtil jwtUtil, UserRepository userRepository, SessionRepository sessionRepository) {
    this.jwtUtil = jwtUtil;
    this.userRepository = userRepository;
    this.sessionRepository = sessionRepository;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith("Bearer ")) {
      String token = header.substring(7);
      if (jwtUtil.isValid(token)) {
        Claims claims = jwtUtil.parse(token);
        UUID userId = UUID.fromString(claims.getSubject());
        UUID tenantId = UUID.fromString(claims.get("tenant_id", String.class));

        String sessionHeader = request.getHeader("X-Session-Id");
        if (sessionHeader != null && !sessionHeader.isBlank()) {
          try {
            UUID sessionId = UUID.fromString(sessionHeader);
            var session = sessionRepository.findById(sessionId);
            if (session.isEmpty() || !session.get().getUserId().equals(userId)) {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              return;
            }
          } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
          }
        }

        TenantCtx.set(tenantId);
        userRepository.findByIdAndTenantId(userId, tenantId).ifPresent(user -> {
          var principal = new LoggedInUser(user);
          var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
          SecurityContextHolder.getContext().setAuthentication(auth);
        });
      }
    }
    chain.doFilter(request, response);
  }
}
