package com.example.saas.util;

import java.util.UUID;

public final class TenantCtx {
  private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

  private TenantCtx() {
  }

  public static void set(UUID tenantId) {
    CURRENT.set(tenantId);
  }

  public static UUID get() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
