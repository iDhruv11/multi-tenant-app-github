package com.example.saas.filter;

import com.example.saas.util.TenantCtx;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantFilter extends OncePerRequestFilter {

  private static final String TENANT_HEADER = "X-Tenant-ID";

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String tenantHeader = request.getHeader(TENANT_HEADER);

    if (tenantHeader != null && !tenantHeader.isBlank()) {
      try {
        UUID tenantId = UUID.fromString(tenantHeader);
        TenantCtx.set(tenantId);
      } catch (IllegalArgumentException e) {
        // nothing lol
      }
    }

    try {
      // Forward
      filterChain.doFilter(request, response);
    } finally {
      TenantCtx.clear();
    }
  }
}
