package com.example.saas.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
public class TenantRls {

  @PersistenceContext
  private EntityManager entityManager;

  @Before("@annotation(transactional)")
  public void setRls(Transactional transactional) {
    UUID tenantId = TenantCtx.get();
    if (tenantId != null) {
      // Seeding
      entityManager
          .createNativeQuery("SELECT set_config('app.tenant_id', :tid, true)")
          .setParameter("tid", tenantId.toString())
          .getSingleResult();
    }
  }
}
