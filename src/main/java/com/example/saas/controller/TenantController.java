package com.example.saas.controller;

import com.example.saas.service.TenantService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

  private final TenantService tenantService;

  public TenantController(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @PostMapping("/register")
  public String register(@RequestParam String name, @RequestParam String slug) {
    tenantService.createTenant(name, slug);
    return "Tenant registered successfully";
  }
}
