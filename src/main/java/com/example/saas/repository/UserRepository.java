package com.example.saas.repository;

import com.example.saas.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  
    long countByTenantId(UUID tenantId);
    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    List<User> findByTenantId(UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);
}
