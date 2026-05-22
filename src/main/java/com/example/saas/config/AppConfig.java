package com.example.saas.config;

import com.example.saas.dto.Dtos.ApiResponse;
import com.example.saas.util.TenantCtx;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class AppConfig implements WebMvcConfigurer {

  @Bean
  public OncePerRequestFilter tenantCleanupFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain chain)
          throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
          requestId = UUID.randomUUID().toString();
        }
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
          chain.doFilter(request, response);
        } finally {
          // Guarantees ThreadLocal cleanup regardless of route success/failure
          TenantCtx.clear();
        }
      }
    };
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .resourceChain(true)
        .addResolver(new PathResourceResolver() {
          @Override
          protected Resource getResource(String path, Resource location) throws IOException {
            Resource requested = location.createRelative(path);
            if (requested.exists() && requested.isReadable()) {
              return requested;
            }
            if (path.startsWith("api/") || path.startsWith("swagger") || path.startsWith("v3/")) {
              return null;
            }
            return new ClassPathResource("/static/index.html");
          }
        });
  }
}

@RestControllerAdvice(basePackages = "com.example.saas.controller")
class WrapAdvice implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    Class<?> type = returnType.getParameterType();
    return !type.equals(ApiResponse.class) && !type.equals(void.class) && !ProblemDetail.class.isAssignableFrom(type);
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      org.springframework.http.MediaType contentType,
      Class<? extends HttpMessageConverter<?>> converterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    String requestId = null;
    if (request instanceof ServletServerHttpRequest servlet) {
      HttpServletRequest req = servlet.getServletRequest();
      requestId = (String) req.getAttribute("requestId");
    }
    return new ApiResponse<>(body, Instant.now(), requestId);
  }
}
