package com.example.saas.service;

import com.example.saas.model.Tenant;
import com.example.saas.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

  private final TenantRepository tenantRepository;

  public TenantService(TenantRepository tenantRepository) {
    this.tenantRepository = tenantRepository;
  }

  @Transactional
  public Tenant createTenant(String name, String slug) {
    if (tenantRepository.existsBySlug(slug)) {
      throw new RuntimeException("Slug already taken");
    }

    Tenant tenant = new Tenant();
    tenant.setName(name);
    tenant.setSlug(slug);

    return tenantRepository.save(tenant);
  }
}
